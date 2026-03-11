# Getting Started with CBindingKMP

This guide will walk you through the process of setting up CBindingKMP and calling your first native C function from a Kotlin Multiplatform project.

## Prerequisites

Before you begin, ensure you have the following installed:
-   **JDK 11 or higher**
-   **Android Studio** or **IntelliJ IDEA**
-   **CMake** (for Android/JVM builds)
-   **Kotlin Multiplatform Mobile** plugin (recommended for Android Studio)
-   **LLVM/Clang** (for iOS/Native builds)

## Installation

To use CBindingKMP in your project, apply the Gradle plugin in your `shared/build.gradle.kts`:

```kotlin
plugins {
    id("com.abyxcz.cbinding") version "1.0.0"
}

kotlin {
    // Configure your KMP targets (android, ios, jvm, etc.)
}

// Register the JNI generation task
val generateJni = tasks.named("generateJni", com.abyxcz.buildlogic.JniGeneratorTask::class) {
    inputDir.set(file("../native/c")) // Directory containing your .h and .c files
}

// Add generated code to the Android source set
kotlin {
    sourceSets {
        androidMain {
            kotlin.srcDir(generateJni.map { it.outputDir })
        }
    }
}
```

## Your First Native Function

### 1. Define the C Function

Create a header file at `native/c/mylib.h`:

```c
// native/c/mylib.h
int add_numbers(int a, int b);
```

Implement it in `native/c/mylib.c`:

```c
// native/c/mylib.c
#include "mylib.h"

int add_numbers(int a, int b) {
    return a + b;
}
```

### 2. Build the Project

Run the Gradle build command. The `generateJni` task will automatically parse your header and generate the necessary bridge code.

```bash
./gradlew :shared:assemble
```

### 3. Call from Kotlin

In your shared Kotlin code, use the `NativeLoader` to load the library and call the generated function:

```kotlin
import com.abyxcz.cbindingkmp.shared.NativeLoader
import com.abyxcz.cbindingkmp.shared.generated.add_numbersJNI

fun performAddition(a: Int, b: Int): Int {
    NativeLoader.load() // Load the native library on supported platforms
    return add_numbersJNI(a, b) // Call the generated JNI function
}
```

> [!NOTE]
> On iOS, the `NativeLoader.load()` call is a no-op as the symbols are statically linked. The generated `add_numbersJNI` function bridges the platform differences for you.

## Next Steps

Check out the [Developer Guide](developer-guide.md) for more advanced usage and configuration.
