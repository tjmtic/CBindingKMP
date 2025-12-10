package com.abyxcz.sampleapplication.shared

actual object NativeLoader {
    actual fun load() {
        System.loadLibrary("example_lib")
    }
}
