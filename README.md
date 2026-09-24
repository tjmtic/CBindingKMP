# C-Binding KMP Library Automation

[![Kotlin](https://img.shields.io/badge/kotlin-2.2.21-blue.svg?logo=kotlin)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Documentation](https://img.shields.io/badge/docs-latest-informational)](https://abyxcz.github.io/CBindingKMP/docs/introduction.html)

This project provides a **unified and automated way to include and call native C code from Kotlin Multiplatform (KMP)**.

---

### [🚀 Visit the Landing Page](https://abyxcz.github.io/CBindingKMP/)
### [📖 Read the Full Documentation](docs/introduction.md)

---

## 🎯 Goal

The goal is to allow developers to maintain a single C/C++ codebase (e.g., `src/native/c`) and automatically wire it up to:
- **Android**: Via JNI (Java Native Interface) using CMake.
- **iOS / Native**: Via Kotlin/Native CInterop.
- **Desktop (JVM)**: Via JNI (shared library).

## ✨ Key Features

- **🚀 Generated Glue**: JNI bridge C and Kotlin `external fun` bindings generated from your headers, with spec-correct symbol mangling.
- **📦 Real Buffers**: Zero-copy direct `ByteBuffer` marshalling plus primitive-array overloads (`GetPrimitiveArrayCritical`) — pass pixel/model buffers to C, get results back.
- **🔩 Opaque Handles**: `typedef struct X X;` handles cross the boundary as `Long`.
- **🍏 iOS Static Linking**: per-target static archives via `staticLibraries`, plus a documented recipe for third-party xcframeworks (inference runtimes etc.).
- **⚙️ `cbinding {}` DSL**: configure headers, include names, JNI package and output; source sets are wired automatically.

The generator supports a **documented C subset** (scalars, primitive pointers, opaque handles) and fails loudly on anything else — see [docs/supported-c-subset.md](docs/supported-c-subset.md).

## 📁 Project Structure

- **[`native/c`](native/c)**: Contains your C source code (`mylib.c`, `mylib.h`).
- **[`shared`](shared)**: The KMP library, wired to use the generated bindings.
- **[`plugin`](plugin)**: The Gradle plugin module containing the automation logic.
- **[`docs`](docs)**: Full documentation set for users and developers.

## 🚀 Quick Start

### 1. Define your C function
```c
// native/c/mylib.h
int add_numbers(int a, int b);
```

### 2. Configure the generator
The plugin is published to GitHub Packages. Add the repository to `pluginManagement`
(reading GitHub Packages needs a token with `read:packages`, via `GPR_USER`/`GPR_KEY`
or `gpr.user`/`gpr.key` in `~/.gradle/gradle.properties`):
```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/tjmtic/CBindingKMP")
            credentials {
                username = System.getenv("GPR_USER") ?: providers.gradleProperty("gpr.user").orNull
                password = System.getenv("GPR_KEY") ?: providers.gradleProperty("gpr.key").orNull
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}
```
Working on the plugin itself? `includeBuild("../CBindingKMP/plugin")` inside `pluginManagement`
substitutes a sibling checkout instead.

```kotlin
// build.gradle.kts
plugins { id("com.abyxcz.cbinding") version "1.1.0" }

cbinding {
    headersDir.set(file("native/c"))
    includeHeaders.set(listOf("mylib.h"))
    jniPackage.set("com.example.generated")
}
```

### 3. Build the project
```bash
./gradlew :shared:assemble
```

### 4. Call from Kotlin
```kotlin
import com.abyxcz.cbindingkmp.shared.generated.add_numbersJNI

val result = add_numbersJNI(10, 20)
```

For more details, see the [Getting Started Guide](docs/getting-started.md).

## 📘 Documentation Index

- [Introduction](docs/introduction.md)
- [Getting Started](docs/getting-started.md)
- [Developer Guide](docs/developer-guide.md)
- [Architecture & Automation Logic](docs/architecture.md)
- [API Reference](docs/api-reference.md)
- [Contributing](docs/contributing.md)

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
