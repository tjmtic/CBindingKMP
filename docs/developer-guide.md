# Developer Guide

This guide describes the project structure and development workflow for those who want to contribute to or extend CBindingKMP.

## Project Structure

-   **`native/c/`**: This is where all your shared C source code resides.
    -   `mylib.h`: Public API of your native library.
    -   `mylib.c`: Implementation details.
    -   `CMakeLists.txt`: Build configuration for Android and JVM.
-   **`shared/`**: The core Kotlin Multiplatform module.
    -   `build.gradle.kts`: Configures targets and wires the JNI generator plugin.
    -   `src/commonMain/`: Shared logic and `expect` declarations.
    -   `src/androidMain/`, `src/iosMain/`, `src/jvmMain/`: Platform-specific `actual` implementations.
-   **`plugin/`**: The custom Gradle plugin that powers the automation.
    -   `JniGeneratorTask.kt`: Contains the logic for parsing headers and generating C/Kotlin bridge code.

## Development Workflow

### Adding a New Native Function

1.  **Modify C Header**: Add the function declaration to `native/c/mylib.h`.
2.  **Implement in C**: Add the implementation to `native/c/mylib.c`.
3.  **Run Build**: Run `./gradlew :shared:assemble` to trigger the code generation.
4.  **Verify Generated Code**: Check `shared/build/generated/jni/` to see the updated `jni_gen_bridge.c` and `GeneratedNative.kt`.
5.  **Use in Kotlin**: Call the new `...JNI` function in your Kotlin source.

### Modifying the JNI Generator

If you need to support more complex types or change the binding logic:
1.  Navigate to the `plugin` module.
2.  Update `JniGeneratorTask.kt`.
3.  Rebuild the project. The plugin changes will take effect on the next `generateJni` task execution.

## Build and Testing

### Building the Native Libraries

Android and JVM targets use CMake. You can manually trigger the CMake build via Gradle:

```bash
./gradlew :shared:externalNativeBuildDebug
```

### Running Tests

Run the KMP tests across all targets:

```bash
./gradlew :shared:allTests
```

## Best Practices

-   **Keep Headers Simple**: The current parser is optimized for standard C types (`int`, `void`, `float`, `double`). For complex structures, consider using opaque pointers or flattening your API.
-   **Unified Naming**: Use consistent naming for your C functions to ensure deterministic generation of Kotlin bindings.
-   **Manual Loading**: Always call `NativeLoader.load()` before accessing any native function to ensure the shared library is loaded on Android and JVM.
