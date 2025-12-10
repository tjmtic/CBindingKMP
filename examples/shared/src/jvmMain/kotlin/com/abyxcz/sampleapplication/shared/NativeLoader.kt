package com.abyxcz.sampleapplication.shared

actual object NativeLoader {
    actual fun load() {
        try {
            System.loadLibrary("example_lib")
        } catch (e: UnsatisfiedLinkError) {
             // For simplicity in tests/preview, we might swallow or log.
             // In real app, we handle pathing.
             println("Failed to load library: ${e.message}")
        }
    }
}
