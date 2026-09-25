package com.abyxcz.buildlogic

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Runs the real task action against archives served from file:// URLs. */
class FetchPrebuiltTaskTest {

    private val work: File = Files.createTempDirectory("fetch-prebuilt").toFile()

    private fun project(): Project = ProjectBuilder.builder().withProjectDir(File(work, "p").apply { mkdirs() }).build()

    /** Xcframework layout as shipped by TFLite (tar.gz, nested) and llama.cpp (zip). */
    private fun xcframeworkTree(root: File, name: String, slices: List<String>) {
        slices.forEach { slice ->
            File(root, "pkg-1.0/Frameworks/$name.xcframework/$slice/$name.framework/Headers").mkdirs()
            File(root, "pkg-1.0/Frameworks/$name.xcframework/$slice/$name.framework/Headers/$name.h").writeText("int f(void);")
            File(root, "pkg-1.0/Frameworks/$name.xcframework/$slice/$name.framework/$name").writeText("lib-$slice")
        }
        File(root, "pkg-1.0/LICENSE").writeText("license")
    }

    private fun zip(src: File, out: File): File {
        ZipOutputStream(out.outputStream()).use { z ->
            src.walkTopDown().filter { it.isFile }.forEach { f ->
                z.putNextEntry(ZipEntry(f.relativeTo(src).invariantSeparatorsPath))
                f.inputStream().use { it.copyTo(z) }
                z.closeEntry()
            }
        }
        return out
    }

    private fun targz(src: File, out: File): File {
        val p = ProcessBuilder("tar", "czf", out.absolutePath, "-C", src.absolutePath, ".").inheritIO().start()
        check(p.waitFor() == 0) { "tar failed" }
        return out
    }

    private fun fetch(project: Project, configure: FetchPrebuiltTask.() -> Unit): FetchPrebuiltTask {
        val task = project.tasks.register("fetch${System.nanoTime()}", FetchPrebuiltTask::class.java) {
            cacheDir.set(File(work, "cache"))
            configure()
        }.get()
        task.fetch()
        return task
    }

    @Test
    fun `xcframework from a tar-gz lands at the destination with its slices`() {
        val src = File(work, "src").apply { xcframeworkTree(this, "Foo", listOf("ios-arm64", "ios-arm64_x86_64-simulator")) }
        val archive = targz(src, File(work, "Foo-1.0.tar.gz"))
        val out = File(work, "third_party/Foo.xcframework")
        fetch(project()) {
            url.set(archive.toURI().toString())
            sha256.set(PrebuiltArchives.sha256(archive))
            xcframeworkName.set("Foo")
            requiredSlices.set(listOf("ios-arm64", "ios-arm64_x86_64-simulator"))
            outputDir.set(out)
        }
        assertEquals("int f(void);", File(out, "ios-arm64/Foo.framework/Headers/Foo.h").readText())
        assertEquals("lib-ios-arm64_x86_64-simulator", File(out, "ios-arm64_x86_64-simulator/Foo.framework/Foo").readText())
        assertFalse(File(out, "LICENSE").exists(), "only the xcframework is kept")
        assertTrue(File(work, "cache/${PrebuiltArchives.sha256(archive)}/Foo-1.0.tar.gz").exists(), "archive cached by hash")
    }

    @Test
    fun `cached archive is reused without the source`() {
        val src = File(work, "src").apply { xcframeworkTree(this, "Foo", listOf("ios-arm64", "ios-arm64-simulator")) }
        val archive = zip(src, File(work, "Foo.zip"))
        val pin = PrebuiltArchives.sha256(archive)
        val cfg: FetchPrebuiltTask.() -> Unit = {
            url.set(archive.toURI().toString())
            sha256.set(pin)
            xcframeworkName.set("Foo")
            requiredSlices.set(listOf("ios-arm64", "ios-arm64-simulator"))
            outputDir.set(File(work, "out"))
        }
        fetch(project(), cfg)
        archive.delete()
        File(work, "out").deleteRecursively()
        fetch(project(), cfg)
        assertTrue(File(work, "out/ios-arm64-simulator/Foo.framework/Headers/Foo.h").exists())
    }

    @Test
    fun `wrong pin fails and leaves nothing behind`() {
        val src = File(work, "src").apply { xcframeworkTree(this, "Foo", listOf("ios-arm64")) }
        val archive = zip(src, File(work, "Foo.zip"))
        val e = assertFailsWith<IllegalStateException> {
            fetch(project()) {
                url.set(archive.toURI().toString())
                sha256.set("00".repeat(32))
                xcframeworkName.set("Foo")
                outputDir.set(File(work, "out"))
            }
        }
        assertTrue(e.message!!.contains("sha256 mismatch"))
        assertFalse(File(work, "cache/${"00".repeat(32)}/Foo.zip").exists())
        assertFalse(File(work, "out/ios-arm64").exists())
    }

    @Test
    fun `remote archive without a pin is refused`() {
        val e = assertFailsWith<IllegalStateException> {
            fetch(project()) {
                url.set(File(work, "x.zip").toURI().toString())
                outputDir.set(File(work, "out"))
            }
        }
        assertTrue(e.message!!.contains("must be pinned"))
    }

    @Test
    fun `missing slice fails naming the slices present`() {
        val src = File(work, "src").apply { xcframeworkTree(this, "Foo", listOf("ios-arm64", "ios-arm64-simulator")) }
        val archive = zip(src, File(work, "Foo.zip"))
        val e = assertFailsWith<IllegalStateException> {
            fetch(project()) {
                url.set(archive.toURI().toString())
                sha256.set(PrebuiltArchives.sha256(archive))
                xcframeworkName.set("Foo")
                requiredSlices.set(listOf("ios-arm64", "ios-arm64_x86_64-simulator"))
                outputDir.set(File(work, "out"))
            }
        }
        assertTrue(e.message!!.contains("it has ios-arm64, ios-arm64-simulator"))
    }

    @Test
    fun `local aar keeps only the included trees`() {
        val src = File(work, "aar").apply {
            File(this, "headers/tensorflow/lite").mkdirs()
            File(this, "headers/tensorflow/lite/c_api.h").writeText("h")
            File(this, "jni/arm64-v8a").mkdirs()
            File(this, "jni/arm64-v8a/libtensorflowlite_jni.so").writeText("so")
            File(this, "classes.jar").writeText("jar")
            File(this, "AndroidManifest.xml").writeText("m")
        }
        val aar = zip(src, File(work, "litert-1.4.2.aar"))
        val out = File(work, "android")
        fetch(project()) {
            localArchive.from(aar)
            includes.set(listOf("headers/**", "jni/**"))
            outputDir.set(out)
        }
        assertTrue(File(out, "headers/tensorflow/lite/c_api.h").exists())
        assertTrue(File(out, "jni/arm64-v8a/libtensorflowlite_jni.so").exists())
        assertFalse(File(out, "classes.jar").exists())
        assertFalse(File(out, "AndroidManifest.xml").exists())
    }

    @Test
    fun `source archive strips its single top-level directory`() {
        val src = File(work, "srcpkg").apply {
            File(this, "llama.cpp-b1/include").mkdirs()
            File(this, "llama.cpp-b1/include/llama.h").writeText("h")
            File(this, "llama.cpp-b1/CMakeLists.txt").writeText("c")
        }
        val archive = targz(src, File(work, "b1.tar.gz"))
        val out = File(work, "llama")
        fetch(project()) {
            url.set(archive.toURI().toString())
            sha256.set(PrebuiltArchives.sha256(archive))
            outputDir.set(out)
        }
        assertTrue(File(out, "include/llama.h").exists())
        assertTrue(File(out, "CMakeLists.txt").exists())
    }
}
