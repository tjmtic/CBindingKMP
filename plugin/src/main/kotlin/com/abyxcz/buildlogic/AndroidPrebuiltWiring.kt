package com.abyxcz.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Project
import java.io.File

/** Loaded only when an Android Gradle plugin is applied. */
internal object AndroidPrebuiltWiring {
    /**
     * Android: CMake receives `-DCBINDING_JNI_BRIDGE=<generated bridge>` and, per prebuilt,
     * `-D<cmakeVariable>=<extracted dir>`; the extraction runs before preBuild.
     */
    fun wire(project: Project, extension: CBindingExtension) {
        @Suppress("UNCHECKED_CAST")
        val components = project.extensions.getByType(AndroidComponentsExtension::class.java)
            as AndroidComponentsExtension<CommonExtension<*, *, *, *, *, *>, *, *>
        components.finalizeDsl { dsl ->
            val args = dsl.defaultConfig.externalNativeBuild.cmake.arguments
            val bridge = File(extension.outputDir.get().asFile, "jni_gen_bridge.c").absolutePath
            args += "-DCBINDING_JNI_BRIDGE=$bridge"
            extension.prebuilts.filter { it.android.isConfigured }.forEach { spec ->
                args += "-D${spec.android.cmakeVariable.get()}=${spec.android.destination.get().asFile.absolutePath}"
                val fetch = spec.androidFetchTaskName
                project.tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(fetch) }
            }
        }
    }
}
