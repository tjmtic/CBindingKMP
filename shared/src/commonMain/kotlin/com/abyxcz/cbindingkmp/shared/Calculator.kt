package com.abyxcz.cbindingkmp.shared

// Load the library logic
@Suppress("unused") // Trigger static init
private val loader = NativeLoader.load()

// We need to define the JNI function for Android/JVM if we use direct JNI mapping.
// However, JNI mapping requires the C function to be named Java_package_Class_method.
// Our sample C code usage `add_numbers`.
// To bridge this without changing the C code (which was the goal: "users maintain a c codebase"),
// we need an intermediate layer OR use JNA/JNR for JVM/Android, or use @CName (Native) in K/N.
//
// For Android/JVM JNI: standard JNI requires JNI naming convention.
// If we want to call `add_numbers`, we typically write a JNI wrapper.
// Automation of THAT wrapper is part of the "glue".
//
// For this demo, since we can't easily generate the C JNI wrapper file on the fly without a compiler plugin,
// I will implement a JNI wrapper in Kotlin using `external fun` but that expects the C symbol to match.
//
// WAIT, for Native (iOS), cinterop generates the bindings directly to `add_numbers`.
// For Android, we MUST have JNI compliant names.
//
// Strategy: Use the `mylib.c` as the "core".
// For Android, we need a separate `jni_wrapper.c` that calls `mylib.c`.
// I will create that wrapper manually now to show the concept, as "automation" implies we'd generate it.
// I will add `jni_wrapper.c` to the `native/c` folder and include it in CMake.

expect fun addNumbers(a: Int, b: Int): Int
