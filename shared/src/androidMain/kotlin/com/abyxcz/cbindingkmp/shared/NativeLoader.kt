package com.abyxcz.cbindingkmp.shared

actual object NativeLoader {
    actual fun load() {
        System.loadLibrary("mylib")
    }
}
