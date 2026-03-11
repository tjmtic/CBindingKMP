# Introduction to CBindingKMP

CBindingKMP is a powerful automation framework designed to bridge the gap between native C/C++ code and Kotlin Multiplatform (KMP) applications. It eliminates the tedious and error-prone boilerplate associated with JNI (Java Native Interface) and CInterop, allowing developers to focus on writing performance-critical logic once and calling it seamlessly from all KMP targets.

## The Problem

Integrating native C code into a KMP project typically requires:
1.  **Android**: Writing manual JNI wrappers (C functions with specific naming conventions) and using CMake to build shared libraries.
2.  **iOS/Native**: Configuring `.def` files and CInterop to generate Kotlin bindings.
3.  **JVM/Desktop**: Similar to Android, requiring manual JNI work and shared library loading.

This redundancy leads to maintenance headaches, as every change to the C API must be manually synchronized across multiple glue layers.

## The Solution

CBindingKMP provides an **automated bridge generator**. By pointing the plugin to your C headers, it automatically:
-   **Parses C function declarations**.
-   **Generates JNI-compliant C wrappers** for Android and JVM.
-   **Generates Kotlin `external fun` declarations** that map directly to the JNI wrappers.
-   **Wires everything into the Gradle build cycle**, ensuring that changes to your C code are reflected in your Kotlin API instantly.

## Key Features

-   **🚀 Zero Glue Code**: Stop writing JNI boilerplate. Let the automation handle it.
-   **🛠 Unified Source**: Maintain a single C source directory for all platforms.
-   **📱 Platform Support**: Out-of-the-box support for Android, iOS, JVM, and Native targets.
-   **⚙️ Gradle Integrated**: Seamlessly fits into your existing KMP build workflow.
-   **🔒 Type Safety**: Generated Kotlin bindings ensure type safety when calling native code.

## Why CBindingKMP?

For performance-intensive tasks like image processing, cryptography, or game engines, native C code is often required. CBindingKMP makes these technologies accessible to KMP developers without the "native tax" of manual binding maintenance.
