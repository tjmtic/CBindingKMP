package com.abyxcz.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Thin Gradle shell over the pure parser/codegen pipeline:
 * [CHeaderParser] -> [JniCodegen] + [KotlinCodegen].
 */
abstract class JniGeneratorTask : DefaultTask() {

    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    /** Header names to #include in the generated bridge. */
    @get:Input
    abstract val includeHeaders: ListProperty<String>

    /** Package of the generated Kotlin bindings (drives the JNI symbol prefix). */
    @get:Input
    abstract val jniPackage: Property<String>

    /** Base name of the generated Kotlin file. */
    @get:Input
    abstract val kotlinFileName: Property<String>

    init {
        // Defaults preserve historical behavior; the extension overrides them.
        includeHeaders.convention(listOf("mylib.h"))
        jniPackage.convention("com.abyxcz.cbindingkmp.shared.generated")
        kotlinFileName.convention("GeneratedNative")
    }

    @TaskAction
    fun generate() {
        val input = inputDir.get().asFile
        val output = outputDir.get().asFile
        output.mkdirs()

        val headerFiles = input.listFiles { file -> file.name.endsWith(".h") }
            ?.sortedBy { it.name }
            ?: emptyList()

        val model = CHeaderParser.parseAll(headerFiles.map { it.readText() })
        val config = CodegenConfig(
            includeHeaders = includeHeaders.get(),
            jniPackage = jniPackage.get(),
            kotlinFileName = kotlinFileName.get()
        )

        File(output, "jni_gen_bridge.c").writeText(JniCodegen.generate(model, config))
        File(output, "${config.kotlinFileName}.kt").writeText(KotlinCodegen.generate(model, config))
    }
}
