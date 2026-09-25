package com.abyxcz.cbindingkmp.shared

// No C on this target: the same behaviour in Kotlin.
actual fun greet(name: String, capacity: Int): String = "Hello, $name!"

actual fun utf8Length(text: String): Int = text.count { !it.isLowSurrogate() }
