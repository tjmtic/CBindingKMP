# C-Binding KMP Library Automation

[![Kotlin](https://img.shields.io/badge/kotlin-1.9.22-blue.svg?logo=kotlin)](https://kotlinlang.org/)
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

- **🚀 Zero Glue Code**: Automatically generate JNI wrappers and Kotlin bindings.
- **🛠 Unified Source**: All C code lives in `native/c`. No more multiple glue layers.
- **📱 Multi-Platform**: Support for Android, iOS, JVM, and Native targets out-of-the-box.
- **⚙️ Gradle Integrated**: Custom `generateJni` task fits seamlessly into your build workflow.

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

### 2. Build the project
```bash
./gradlew :shared:assemble
```

### 3. Call from Kotlin
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
