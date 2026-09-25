package com.abyxcz.cbindingkmp.shared

import kotlin.test.Test
import kotlin.test.assertEquals

/** Real C on the Kotlin/Native simulator target, through cinterop. */
class StringRoundtripTest {
    // Latin-1, a 4-byte emoji, a 3-byte symbol and CJK: every UTF-8 sequence length.
    private val name = "Zoë 🌍 ☃ 東京"

    @Test
    fun stringInCountsCodePointsNotBytesOrUtf16Units() {
        assertEquals(10, utf8Length(name))
        assertEquals(0, utf8Length(""))
    }

    @Test
    fun stringOutRoundTripsEveryUtf8Length() {
        assertEquals("Hello, $name!", greet(name))
    }

    @Test
    fun tooSmallBufferIsResizedOnce() {
        assertEquals("Hello, $name!", greet(name, capacity = 4))
    }
}
