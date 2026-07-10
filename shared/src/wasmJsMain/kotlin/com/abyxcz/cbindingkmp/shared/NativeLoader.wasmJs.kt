package com.abyxcz.cbindingkmp.shared

actual object NativeLoader {
    actual fun load() {
        // No native library on wasmJs.
    }
}
