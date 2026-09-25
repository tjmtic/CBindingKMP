# Supported C Subset

The binding generator intentionally parses a **defined subset** of C declarations. Anything
outside it fails the build with the offending declaration quoted — silent fallback produced
unusable bindings in earlier versions.

## Accepted

- Line (`//`) and block (`/* */`) comments, preprocessor lines (`#...`): stripped.
- Declarations spanning multiple lines (statements end at `;`).
- Opaque handle typedefs, self-aliased: `typedef struct bd_ctx bd_ctx;`
- Function declarations using:
  - **Return types**: `void`, `int`, `int32_t`, `int64_t`, `size_t`, `float`, `double`,
    or an opaque handle pointer (`bd_ctx*`).
  - **Parameter types**: the scalars above, opaque handle pointers (`bd_ctx*`,
    `const bd_ctx*`), and primitive pointers `uint8_t*`, `float*`, `int32_t*`
    (each optionally `const`).
  - `(void)` as an empty parameter list.
- Strings (since 1.3.0):
  - `const char*` parameters: NUL-terminated UTF-8 input.
  - One out-string buffer: `char* out, int32_t cap` as the **last two** parameters of a
    function returning `int32_t` — see [Strings](#strings).

## Rejected (build error)

Structs by value, string *return* types (`const char*`/`char*` — nobody knows who frees
them), `char**` and other arrays of strings, a `char*` anywhere but the out-string position,
function pointers, arrays, nested pointers, varargs, `unsigned`/`long` spellings outside the
list above, primitive-pointer *return* types, non-self-aliasing typedefs.

## Marshalling (Android JNI)

| C type | Kotlin external | JNI mechanism |
|---|---|---|
| scalar | `Int`/`Long`/`Float`/`Double` | by value |
| `X*` opaque | `Long` | `intptr_t` cast |
| `const uint8_t*` | `java.nio.ByteBuffer` (primary) | `GetDirectBufferAddress` — **must be direct**, throws otherwise; zero-copy, safe for long native calls |
| `const uint8_t*` | `ByteArray` (`...Arr` overload) | `GetPrimitiveArrayCritical` + `JNI_ABORT` release |
| `uint8_t*` / `float*` / `int32_t*` | `ByteArray`/`FloatArray`/`IntArray` | `GetPrimitiveArrayCritical` + write-back release |
| `const char*` | `String` (wrapper) → NUL-terminated UTF-8 `ByteArray` (raw) | `GetByteArrayElements` + `JNI_ABORT` |
| `char* out, int32_t cap` | `String` result (wrapper) → `ByteArray` + `Int` (raw) | `GetByteArrayElements` + copy-back release |

**Critical-section caveat**: `GetPrimitiveArrayCritical` may pause GC for the duration of
the native call. Pass large, long-lived buffers (camera frames, model files) as direct
`ByteBuffer`s via the primary variant; use arrays only for small in/out parameters.

iOS is bound via Kotlin/Native cinterop directly against the same header — no generated
glue needed; see `docs/ios-prebuilt-linking.md` and the facade recipe below.

## Strings

```c
int32_t utf8_length(const char* text);                    // input only
int32_t greet(const char* name, char* out, int32_t cap);  // input + out-string
```

For every function that carries text the generator emits a raw external and a wrapper:

```kotlin
internal external fun greetUtf8JNI(name: ByteArray, out: ByteArray, cap: Int): Int
internal fun greetJNI(name: String, capacity: Int = 256): String   // what you call
```

**The out-string contract** your C function must keep:

- Write UTF-8 into `out` (a NUL terminator is not needed) and return the number of bytes
  written, `0..cap`.
- If `cap` is too small, write nothing and return `-(capacity needed)`. The wrapper
  allocates exactly that and calls **once more**, so a too-small call must not consume
  state: a token generator keeps the pending piece and hands it out on the retry.
- The first buffer is `capacity` bytes (default 256); pass a larger one when you know the
  output is big, to skip the retry.

**Why UTF-8 is encoded in Kotlin.** JNI's `GetStringUTFChars` returns Java's *modified*
UTF-8: a 4-byte character such as 🌍 arrives as two 3-byte surrogate halves, and NUL as
`C0 80`. A tokenizer or any C library expecting real UTF-8 would see different bytes. The
wrapper uses `encodeToByteArray()` / `decodeToString()` instead, and pins the bytes with
`GetByteArrayElements`, not the critical variant, because string calls (prompts, token
generation) are the ones that run long and must not stall the GC.

**Pitfalls.**

- A Kotlin string containing `\u0000` is truncated there, as C sees it.
- `decodeToString` replaces malformed UTF-8 with U+FFFD. A C function that emits text in
  pieces must not split a multi-byte character across two calls. LLM token pieces can end
  mid-character, so buffer the incomplete tail in C and emit it with the next piece.

**iOS.** cinterop maps `const char*` to `String` itself (real UTF-8). The out buffer is
native memory; the same contract, by hand:

```kotlin
@OptIn(ExperimentalForeignApi::class)
actual fun greet(name: String, capacity: Int): String = memScoped {
    var cap = capacity
    var buf = allocArray<ByteVar>(cap)
    var n = cGreet(name, buf, cap)            // import ...cinterop.greet as cGreet
    if (n < 0) { cap = -n; buf = allocArray(cap); n = cGreet(name, buf, cap) }
    check(n in 0..cap)
    buf.readBytes(n).decodeToString()
}
```

The demo module has both sides (`Strings.*.kt`) and round-trip tests that run on the iOS
simulator and on an Android device or emulator (`connectedDebugAndroidTest`).

### Streaming: poll, don't call back

Function pointers stay out of the subset. Stream by polling a context from a coroutine:

```c
typedef struct lm_ctx lm_ctx;
int32_t lm_prompt(lm_ctx* ctx, const char* prompt);          // start; 0 = ok
int32_t lm_next_token(lm_ctx* ctx, char* out, int32_t cap);  // 0 bytes = end of stream
```

```kotlin
fun generate(ctx: Long, prompt: String): Flow<String> = flow {
    check(lm_promptJNI(ctx, prompt) == 0)
    while (true) {
        val piece = lm_next_tokenJNI(ctx)      // blocks for one token
        if (piece.isEmpty()) break
        emit(piece)
    }
}.flowOn(Dispatchers.IO)
```

Cancellation is free (the loop stops asking), backpressure is free (C only works when
asked), and no native thread ever calls into the JVM.

## The expect/actual facade recipe

```kotlin
// commonMain
expect class BallDetector(modelBytes: ByteArray) {
    fun detect(pixels: /* platform-delivered */ ..., out: FloatArray): Int
    fun close()
}

// androidMain — generated externals (direct ByteBuffer primary)
actual class BallDetector actual constructor(modelBytes: ByteArray) {
    private val ctx: Long = bd_createArrJNI(modelBytes, modelBytes.size)
    // pixels as direct ByteBuffer straight from the camera plane: zero-copy
    fun detect(pixels: java.nio.ByteBuffer, out: FloatArray): Int =
        bd_detectJNI(ctx, pixels, out, out.size / 6)
    actual fun close() = bd_destroyJNI(ctx)
}

// iosMain — cinterop symbols, pinning for Kotlin-owned memory
actual class BallDetector actual constructor(modelBytes: ByteArray) {
    private val ctx = modelBytes.usePinned { pinned ->
        bd_create(pinned.addressOf(0).reinterpret(), modelBytes.size)
    }
    fun detect(base: CPointer<UByteVar>, out: FloatArray): Int =
        out.usePinned { pinned ->
            bd_detect(ctx, base, pinned.addressOf(0), out.size / 6)
        }
    actual fun close() = bd_destroy(ctx)
}
```
