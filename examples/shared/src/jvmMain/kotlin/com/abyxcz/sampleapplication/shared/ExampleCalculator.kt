package com.abyxcz.sampleapplication.shared

import com.abyxcz.cbindingkmp.shared.generated.example_addJNI

actual fun exampleAdd(a: Int, b: Int): Int {
    NativeLoader.load()
    return example_addJNI(a, b)
}
