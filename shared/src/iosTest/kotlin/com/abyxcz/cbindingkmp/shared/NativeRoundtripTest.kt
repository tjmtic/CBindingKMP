package com.abyxcz.cbindingkmp.shared

import com.abyxcz.cbindingkmp.cinterop.add_numbers
import com.abyxcz.cbindingkmp.cinterop.rgba_to_gray
import com.abyxcz.cbindingkmp.cinterop.sum_bytes
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Executes the REAL C library on the Kotlin/Native simulator target — proof that the
 * prebuilt static archive is compiled, linked via `staticLibraries`, and that buffers
 * round-trip through pinned Kotlin memory.
 */
@OptIn(ExperimentalForeignApi::class)
class NativeRoundtripTest {

    @Test
    fun scalarCall() {
        assertEquals(12, add_numbers(5, 7))
    }

    @Test
    fun sumBytesOverPinnedBuffer() {
        val data = byteArrayOf(1, 2, 3, 4, 100)
        val sum = data.usePinned { pinned ->
            sum_bytes(pinned.addressOf(0).reinterpret(), data.size)
        }
        assertEquals(110, sum)
    }

    @Test
    fun rgbaToGrayMatchesBt601() {
        // Two pixels: pure red and pure white.
        val rgba = ubyteArrayOf(
            255u, 0u, 0u, 255u,
            255u, 255u, 255u, 255u
        ).toByteArray()
        val out = ByteArray(2)

        rgba.usePinned { pixels ->
            out.usePinned { gray ->
                rgba_to_gray(
                    pixels.addressOf(0).reinterpret(),
                    2,
                    gray.addressOf(0).reinterpret()
                )
            }
        }

        assertEquals((299 * 255 / 1000).toByte(), out[0]) // red -> 76
        assertEquals(255.toByte(), out[1])                // white -> 255
    }

    private fun UByteArray.toByteArray(): ByteArray = ByteArray(size) { this[it].toByte() }
}
