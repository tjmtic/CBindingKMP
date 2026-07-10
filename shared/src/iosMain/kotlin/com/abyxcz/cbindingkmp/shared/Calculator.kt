package com.abyxcz.cbindingkmp.shared

import kotlinx.cinterop.ExperimentalForeignApi
import com.abyxcz.cbindingkmp.cinterop.add_numbers

@OptIn(ExperimentalForeignApi::class)
actual fun addNumbers(a: Int, b: Int): Int {
    return add_numbers(a, b)
}
