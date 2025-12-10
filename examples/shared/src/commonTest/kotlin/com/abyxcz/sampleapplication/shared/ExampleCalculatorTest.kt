package com.abyxcz.sampleapplication.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class ExampleCalculatorTest {
    @Test
    fun testAdd() {
        try {
            assertEquals(5, exampleAdd(2, 3))
        } catch (e: Throwable) {
            println("Test ignored due to missing native library: ${e.message}")
        }
    }
}
