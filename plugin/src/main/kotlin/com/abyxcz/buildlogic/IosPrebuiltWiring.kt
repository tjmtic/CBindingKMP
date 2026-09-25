package com.abyxcz.buildlogic

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.StaticLibrary
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

/** Loaded only when the Kotlin Multiplatform plugin is applied. */
internal object IosPrebuiltWiring {
    /**
     * iOS: every cinterop gets the framework headers and waits for the fetch; every
     * non-static binary links `-F<slice> -framework <name>` plus `-rpath <slice>`, so a
     * dynamic framework is found at run time by K/N test executables (static frameworks
     * and libraries resolve those symbols in the final app link, i.e. Xcode, which embeds
     * a dynamic framework itself).
     */
    fun wire(project: Project, kmp: KotlinMultiplatformExtension, extension: CBindingExtension) {
        val specs = extension.prebuilts.filter { it.ios.isConfigured }
        if (specs.isEmpty()) return
        kmp.targets.withType(KotlinNativeTarget::class.java)
            .matching { it.konanTarget.family == Family.IOS }
            .all {
                val target = this
                val device = target.konanTarget == KonanTarget.IOS_ARM64
                specs.forEach { spec ->
                    val fetch = project.tasks.named(spec.iosFetchTaskName)
                    val slice = spec.ios.sliceDir(device).get().asFile
                    val headers = spec.ios.headersDir(device).get().asFile
                    val flags =
                        listOf("-F${slice.absolutePath}", "-framework", spec.ios.frameworkName.get()) +
                            spec.ios.linkerOpts.get() +
                            listOf("-rpath", slice.absolutePath)
                    target.compilations.all {
                        cinterops.all {
                            includeDirs(headers)
                            val interopTask = interopProcessingTaskName
                            project.tasks.matching { it.name == interopTask }.configureEach { dependsOn(fetch) }
                        }
                    }
                    target.binaries.all {
                        linkTaskProvider.configure { dependsOn(fetch) }
                        val isStatic = (this is Framework && isStatic) || this is StaticLibrary
                        if (!isStatic) linkerOpts(flags)
                    }
                }
            }
    }
}
