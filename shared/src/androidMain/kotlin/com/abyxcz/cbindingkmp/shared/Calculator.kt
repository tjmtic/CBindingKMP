package com.abyxcz.cbindingkmp.shared

import com.abyxcz.cbindingkmp.shared.generated.add_numbersJNI

actual fun addNumbers(a: Int, b: Int): Int {
    return add_numbersJNI(a, b)
}

