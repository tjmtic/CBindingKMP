package com.abyxcz.buildlogic

import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrebuiltDslTest {

    @Test
    fun `declaring a prebuilt registers fetch tasks with conventions`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CBindingPlugin::class.java)
        val ext = project.extensions.getByType(CBindingExtension::class.java)
        val spec = ext.prebuilt("llama-cpp") {
            ios { xcframework("https://x/llama-b1-xcframework.zip", "ab".repeat(32), "llama") }
            android { archive("https://x/b1.tar.gz", "cd".repeat(32)) }
        }

        assertEquals("fetchPrebuiltLlamaCppIos", spec.iosFetchTaskName)
        assertEquals("fetchPrebuiltLlamaCppAndroid", spec.androidFetchTaskName)
        assertTrue(project.tasks.names.containsAll(listOf(spec.iosFetchTaskName, spec.androidFetchTaskName)))
        assertTrue(spec.ios.isConfigured)
        assertTrue(spec.android.isConfigured)
        assertEquals("CBINDING_LLAMA_CPP_DIR", spec.android.cmakeVariable.get())
        assertEquals(listOf("-lc++"), spec.ios.linkerOpts.get())

        val build = project.layout.buildDirectory.get().asFile
        assertEquals(File(build, "cbinding/prebuilt/llama-cpp/ios/llama.xcframework"), spec.ios.xcframeworkDir().get().asFile)
        assertEquals(
            File(build, "cbinding/prebuilt/llama-cpp/ios/llama.xcframework/ios-arm64_x86_64-simulator/llama.framework/Headers"),
            spec.ios.headersDir(device = false).get().asFile,
        )
        val task = project.tasks.getByName(spec.iosFetchTaskName) as FetchPrebuiltTask
        assertEquals(listOf("ios-arm64", "ios-arm64_x86_64-simulator"), task.requiredSlices.get())
        assertEquals(spec.ios.xcframeworkDir().get().asFile, task.outputDir.get().asFile)
    }

    @Test
    fun `aar defaults to headers and jni, into() moves the xcframework`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CBindingPlugin::class.java)
        val ext = project.extensions.getByType(CBindingExtension::class.java)
        val spec = ext.prebuilt("tflite") {
            ios {
                xcframework("https://x/TensorFlowLiteC-2.17.0.tar.gz", "ab".repeat(32), "TensorFlowLiteC")
                into(project.file("native/third_party"))
            }
            android {
                aar("com.google.ai.edge.litert:litert:1.4.2")
                cmakeVariable.set("LITERT_DIR")
            }
        }
        assertEquals(listOf("headers/**", "jni/**"), spec.android.includes.get())
        assertEquals("LITERT_DIR", spec.android.cmakeVariable.get())
        assertEquals(project.file("native/third_party/TensorFlowLiteC.xcframework"), spec.ios.xcframeworkDir().get().asFile)
        assertFalse(ext.prebuilt("other") {}.ios.isConfigured)
    }
}
