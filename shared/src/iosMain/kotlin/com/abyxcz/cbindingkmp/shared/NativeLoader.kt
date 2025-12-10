package com.abyxcz.cbindingkmp.shared

actual object NativeLoader {
    actual fun load() {
        // No-op on iOS, symbols are statically linked
    }
}
