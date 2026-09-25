package com.abyxcz.cbindingkmp.shared

import com.abyxcz.cbindingkmp.cinterop.greet as cGreet
import com.abyxcz.cbindingkmp.cinterop.utf8_length
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes

// cinterop maps `const char*` to String (UTF-8) itself; the out buffer is native memory
// scoped to the call, with the same one-retry sizing as the generated JNI wrapper.
@OptIn(ExperimentalForeignApi::class)
actual fun greet(name: String, capacity: Int): String = memScoped {
    require(capacity > 0) { "capacity must be positive, was $capacity" }
    var cap = capacity
    var buf = allocArray<ByteVar>(cap)
    var n = cGreet(name, buf, cap)
    if (n < 0) {
        cap = -n
        buf = allocArray(cap)
        n = cGreet(name, buf, cap)
    }
    check(n in 0..cap) { "greet: returned $n for a $cap-byte buffer" }
    buf.readBytes(n).decodeToString()
}

@OptIn(ExperimentalForeignApi::class)
actual fun utf8Length(text: String): Int = utf8_length(text)
