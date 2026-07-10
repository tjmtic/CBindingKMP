package com.abyxcz.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class CBindingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("cbinding", CBindingExtension::class.java).apply {
            headersDir.convention(project.layout.projectDirectory.dir("native/c"))
            includeHeaders.convention(listOf("mylib.h"))
            jniPackage.convention("com.abyxcz.cbindingkmp.shared.generated")
            kotlinFileName.convention("GeneratedNative")
            outputDir.convention(project.layout.buildDirectory.dir("generated/jni"))
        }

        val generateJni = project.tasks.register("generateJni", JniGeneratorTask::class.java) {
            inputDir.set(extension.headersDir)
            outputDir.set(extension.outputDir)
            includeHeaders.set(extension.includeHeaders)
            jniPackage.set(extension.jniPackage)
            kotlinFileName.set(extension.kotlinFileName)
        }

        // Generated sources must exist before Android's build pipeline starts.
        project.tasks.configureEach {
            if (name == "preBuild") {
                dependsOn(generateJni)
            }
        }

        // Wire the generated Kotlin into the JVM-backed source sets automatically.
        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            val kmp = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
            kmp.sourceSets
                .matching { it.name == "androidMain" || it.name == "jvmMain" }
                .configureEach {
                    kotlin.srcDir(generateJni.map { it.outputDir })
                }
        }
    }
}
