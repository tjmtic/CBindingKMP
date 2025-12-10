package com.abyxcz.cbindingkmp.shared

actual object NativeLoader {
    actual fun load() {
        // For JVM/Desktop, we generally expect the user to have the lib in java.library.path
        // or we load it from a bundled resource. For simplicity here:
        try {
            System.loadLibrary("mylib")
        } catch (e: UnsatisfiedLinkError) {
            println("Failed to load native library: ${e.message}")
        }
    }
}
