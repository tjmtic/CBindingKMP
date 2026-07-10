package com.abyxcz.cbindingkmp.shared

// No native C on JS: pure-Kotlin fallback keeps the demo API total across targets.
actual fun addNumbers(a: Int, b: Int): Int = a + b
