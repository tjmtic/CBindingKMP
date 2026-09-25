package com.abyxcz.cbindingkmp.shared

import com.abyxcz.cbindingkmp.shared.generated.greetJNI
import com.abyxcz.cbindingkmp.shared.generated.utf8_lengthJNI

private val loaded by lazy { NativeLoader.load() }

// The generated wrappers encode real UTF-8, size the out buffer, and decode.
actual fun greet(name: String, capacity: Int): String {
    loaded
    return greetJNI(name, capacity)
}

actual fun utf8Length(text: String): Int {
    loaded
    return utf8_lengthJNI(text)
}
