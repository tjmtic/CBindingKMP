# C-Binding KMP Library Automation

This project demonstrates a **unified and automated way to include and call native C code from Kotlin Multiplatform (KMP)**.

## Goal

The goal is to allow developers to maintain a single C/C++ codebase (e.g., `src/native/c`) and automatically wire it up to:
- **Android**: Via JNI (Java Native Interface) using CMake.
- **iOS / Native**: Via Kotlin/Native CInterop.
- **Desktop (JVM)**: Via JNI (shared library).

This project provides the "glue" so you can focus on writing C code and calling it from Kotlin.

## Features

- **Unified Native Source**: All C code lives in `native/c`.
- **Automated Build Wiring**:
    - **CMake** is used to build shared libraries (`.so`, `.dylib`, `.dll`) for Android and JVM.
    - **CInterop** is configured to include headers for iOS/Native targets.
- **JNI Generation**: A custom Gradle task (`generateJni`) automatically parses your C headers and generates:
    - `jni_gen_bridge.c`: The JNI compliant C wrappers.
    - `GeneratedNative.kt`: The Kotlin `external fun` declarations.
    - This eliminates the boilerplate of writing manual JNI bindings.
- **Kotlin API Wrapper**: A simple Kotlin API (`Calculator.kt`) that bridges the platform differences (JNI vs Direct CInterop).

## Project Structure

- **`native/c`**: Contains your C source code (`mylib.c`, `mylib.h`).
    - `CMakeLists.txt`: Configures the build for this directory.
- **`shared`**: The KMP library.
    - `build.gradle.kts`: Configures the KMP targets and registers the `generateJni` task.
    - `src/nativeInterop/cinterop`: Configuration for iOS/Native bindings.
    - `src/androidMain`: Wired to use the generated JNI code.
- **`buildSrc`**: Contains the build logic for automation.
    - `src/main/kotlin/.../JniGeneratorTask.kt`: The logic that parses headers and generates code.
- **`composeApp`**: Sample KMP application demonstrating usage.

## Usage

### 1. Add C Functions
Add your function to `native/c/mylib.h`:

```c
// native/c/mylib.h
int add_numbers(int a, int b);
int multiply(int a, int b); // Add new function
```

Implement it in `native/c/mylib.c`.

### 2. Build
Run the build. The `generateJni` task will run automatically before compilation.

```bash
./gradlew :shared:assemble
```

This will:
1. Parse `mylib.h`.
2. Update `shared/build/generated/jni/jni_gen_bridge.c` with a JNI wrapper for `multiply`.
3. Update `shared/build/generated/jni/GeneratedNative.kt` with `external fun multiplyJNI(...)`.
4. Compile the C code via CMake (Android) or CInterop (iOS).

### 3. Call from Kotlin
In your shared Kotlin code (`Calculator.kt` or similar):

**For Android/JVM (JNI):**
```kotlin
import com.abyxcz.cbindingkmp.shared.generated.multiplyJNI

actual fun multiply(a: Int, b: Int): Int {
    return multiplyJNI(a, b)
}
```

**For iOS (Native):**
```kotlin
import com.abyxcz.cbindingkmp.cinterop.multiply

actual fun multiply(a: Int, b: Int): Int {
    return multiply(a, b) // Direct call via cinterop
}
```

## Implementation Details

### JNI Generator
The `JniGeneratorTask` provides a lightweight way to bridge C and JNI. It uses loose regex matching to find function declarations in `.h` files and produces the necessary JNI boilerplate.
- **Input**: `native/c/*.h`
- **Output**:
    - `jni_gen_bridge.c`: Included in the CMake build.
    - `GeneratedNative.kt`: Added to the `androidMain` source set.

### CMake Integration
The `shared/build.gradle.kts` configures the Android plugin to use `externalNativeBuild` pointing to `native/c/CMakeLists.txt`. This ensures the C code (and generated bridge) is compiled into the app's native libraries.

### Native Loading
A helper object `NativeLoader` is used in `commonMain` (expect/actual) to load the shared library (`System.loadLibrary`) on Android/JVM platforms. On iOS, symbols are statically linked so no loader is needed.