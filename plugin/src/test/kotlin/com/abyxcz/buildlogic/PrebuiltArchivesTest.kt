package com.abyxcz.buildlogic

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PrebuiltArchivesTest {

    private fun tmp(): File = Files.createTempDirectory("prebuilt-archives").toFile()

    @Test
    fun `kind is derived from the file name`() {
        assertEquals(PrebuiltArchives.Kind.ZIP, PrebuiltArchives.kindOf("llama-b1-xcframework.zip"))
        assertEquals(PrebuiltArchives.Kind.ZIP, PrebuiltArchives.kindOf("litert-1.4.2.aar"))
        assertEquals(PrebuiltArchives.Kind.TAR_GZ, PrebuiltArchives.kindOf("TensorFlowLiteC-2.17.0.tar.gz"))
        assertEquals(PrebuiltArchives.Kind.TAR_GZ, PrebuiltArchives.kindOf("src.TGZ"))
        assertEquals(PrebuiltArchives.Kind.TAR, PrebuiltArchives.kindOf("x.tar"))
        assertFailsWith<IllegalArgumentException> { PrebuiltArchives.kindOf("x.rar") }
    }

    @Test
    fun `sha256 matches the known digest of abc`() {
        val f = File(tmp(), "abc").apply { writeText("abc") }
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            PrebuiltArchives.sha256(f),
        )
    }

    @Test
    fun `verify reports both hashes on mismatch`() {
        val f = File(tmp(), "abc").apply { writeText("abc") }
        PrebuiltArchives.verify(f, "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD ", "abc")
        val e = assertFailsWith<IllegalStateException> { PrebuiltArchives.verify(f, "00".repeat(32), "https://x/abc") }
        assertTrue(e.message!!.contains("expected: ${"00".repeat(32)}"))
        assertTrue(e.message!!.contains("actual:   ba7816bf"))
        assertTrue(e.message!!.contains("https://x/abc"))
    }

    @Test
    fun `file name comes from the url path, not the query`() {
        assertEquals(
            "llama-b11165-xcframework.zip",
            PrebuiltArchives.fileNameOf("https://github.com/ggml-org/llama.cpp/releases/download/b11165/llama-b11165-xcframework.zip?x=1"),
        )
        assertFailsWith<IllegalArgumentException> { PrebuiltArchives.fileNameOf("https://example.com/") }
    }

    @Test
    fun `finds the named xcframework and names the others when missing`() {
        val root = tmp()
        File(root, "pkg/Frameworks/Foo.xcframework/ios-arm64").mkdirs()
        File(root, "pkg/Frameworks/Bar.xcframework/ios-arm64").mkdirs()
        assertEquals("Foo.xcframework", PrebuiltArchives.findXcframework(root, "Foo").name)
        val e = assertFailsWith<IllegalStateException> { PrebuiltArchives.findXcframework(root, "Baz") }
        assertTrue(e.message!!.contains("Bar.xcframework") && e.message!!.contains("Foo.xcframework"))
    }

    @Test
    fun `missing slice lists the slices that exist`() {
        val xcf = File(tmp(), "Foo.xcframework").apply {
            File(this, "ios-arm64").mkdirs()
            File(this, "ios-arm64-simulator").mkdirs()
        }
        PrebuiltArchives.requireSlices(xcf, listOf("ios-arm64", "ios-arm64-simulator"))
        val e = assertFailsWith<IllegalStateException> {
            PrebuiltArchives.requireSlices(xcf, listOf("ios-arm64", "ios-arm64_x86_64-simulator"))
        }
        assertTrue(e.message!!.contains("no slice ios-arm64_x86_64-simulator"))
        assertTrue(e.message!!.contains("it has ios-arm64, ios-arm64-simulator"))
    }

    @Test
    fun `strips a single top-level directory only`() {
        val one = tmp().apply { File(this, "llama.cpp-b1/include").mkdirs() }
        assertEquals("llama.cpp-b1", PrebuiltArchives.singleRootOrSelf(one).name)
        val two = tmp().apply {
            File(this, "a").mkdirs()
            File(this, "b.txt").writeText("x")
        }
        assertEquals(two, PrebuiltArchives.singleRootOrSelf(two))
    }
}
