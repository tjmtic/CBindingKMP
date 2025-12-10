package com.abyxcz.cbindingkmp.shared

import com.abyxcz.cbindingkmp.cinterop.add_numbers

actual fun addNumbers(a: Int, b: Int): Int {
    return add_numbers(a, b)
}
