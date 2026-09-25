package com.abyxcz.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File

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

        // Generated sources must exist before Android's build pipeline starts. AGP runs
        // configureCMake*/buildCMake* after preBuild, so this also orders the native build.
        project.tasks.configureEach {
            if (name == "preBuild") {
                dependsOn(generateJni)
            }
        }

        registerPrebuiltTasks(project, extension)

        // Wire the generated Kotlin into the JVM-backed source sets automatically.
        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            val kmp = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
            kmp.sourceSets
                .matching { it.name == "androidMain" || it.name == "jvmMain" }
                .configureEach {
                    kotlin.srcDir(generateJni.map { it.outputDir })
                }
            project.afterEvaluate { IosPrebuiltWiring.wire(project, kmp, extension) }
        }

        // Platform wiring lives in separate classes: they name KGP / AGP types, which are only
        // on the classpath when those plugins are applied. Referencing them from this class's
        // own method signatures would break plugin instantiation in projects without them.
        project.pluginManager.withPlugin("com.android.library") { AndroidPrebuiltWiring.wire(project, extension) }
        project.pluginManager.withPlugin("com.android.application") { AndroidPrebuiltWiring.wire(project, extension) }
    }

    /** Conventions + fetch tasks for every declared prebuilt. Tasks only run when wired in. */
    private fun registerPrebuiltTasks(project: Project, extension: CBindingExtension) {
        val cache = File(project.gradle.gradleUserHomeDir, "caches/cbinding-prebuilt")
        extension.prebuilts.all {
            val spec = this
            val constant = spec.name.uppercase().replace(Regex("[^A-Z0-9]"), "_")
            ios.destination.convention(project.layout.buildDirectory.dir("cbinding/prebuilt/${spec.name}/ios"))
            ios.deviceSlice.convention("ios-arm64")
            ios.simulatorSlice.convention("ios-arm64_x86_64-simulator")
            ios.linkerOpts.convention(listOf("-lc++"))
            android.destination.convention(project.layout.buildDirectory.dir("cbinding/prebuilt/${spec.name}/android"))
            android.cmakeVariable.convention("CBINDING_${constant}_DIR")
            android.includes.convention(emptyList())

            project.tasks.register(spec.iosFetchTaskName, FetchPrebuiltTask::class.java) {
                description = "Fetches and verifies the '${spec.name}' xcframework."
                url.set(spec.ios.url)
                sha256.set(spec.ios.sha256)
                xcframeworkName.set(spec.ios.frameworkName)
                requiredSlices.set(spec.ios.deviceSlice.zip(spec.ios.simulatorSlice) { d, s -> listOf(d, s) })
                outputDir.set(spec.ios.xcframeworkDir())
                cacheDir.set(cache)
            }
            project.tasks.register(spec.androidFetchTaskName, FetchPrebuiltTask::class.java) {
                description = "Fetches and extracts the '${spec.name}' Android runtime for CMake."
                url.set(spec.android.url)
                sha256.set(spec.android.sha256)
                includes.set(spec.android.includes)
                outputDir.set(spec.android.destination)
                cacheDir.set(cache)
                localArchive.from(
                    project.provider {
                        spec.android.aarCoordinates.orNull?.let { coordinates ->
                            project.configurations
                                .detachedConfiguration(project.dependencies.create("$coordinates@aar"))
                                .apply { isTransitive = false }
                        } ?: emptyList<File>()
                    }
                )
            }
        }
    }
}
