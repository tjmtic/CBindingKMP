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

## Rejected (build error)

Structs by value, `char*` strings, function pointers, arrays, nested pointers, varargs,
`unsigned`/`long` spellings outside the list above, primitive-pointer *return* types,
non-self-aliasing typedefs.

## Marshalling (Android JNI)

| C type | Kotlin external | JNI mechanism |
|---|---|---|
| scalar | `Int`/`Long`/`Float`/`Double` | by value |
| `X*` opaque | `Long` | `intptr_t` cast |
| `const uint8_t*` | `java.nio.ByteBuffer` (primary) | `GetDirectBufferAddress` — **must be direct**, throws otherwise; zero-copy, safe for long native calls |
| `const uint8_t*` | `ByteArray` (`...Arr` overload) | `GetPrimitiveArrayCritical` + `JNI_ABORT` release |
| `uint8_t*` / `float*` / `int32_t*` | `ByteArray`/`FloatArray`/`IntArray` | `GetPrimitiveArrayCritical` + write-back release |

**Critical-section caveat**: `GetPrimitiveArrayCritical` may pause GC for the duration of
the native call. Pass large, long-lived buffers (camera frames, model files) as direct
`ByteBuffer`s via the primary variant; use arrays only for small in/out parameters.

iOS is bound via Kotlin/Native cinterop directly against the same header — no generated
glue needed; see `docs/ios-prebuilt-linking.md` and the facade recipe below.

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
