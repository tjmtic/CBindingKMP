package com.abyxcz.buildlogic

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Consumer configuration for the C binding generator:
 *
 * ```kotlin
 * cbinding {
 *     headersDir.set(file("native/c"))
 *     includeHeaders.set(listOf("mylib.h"))
 *     jniPackage.set("com.example.generated")
 *     kotlinFileName.set("MyLibNative")
 * }
 * ```
 *
 * The plugin registers a `generateJni` task from these values, hooks it before
 * `preBuild`, and adds the generated Kotlin to the `androidMain`/`jvmMain` source
 * sets automatically when the Kotlin Multiplatform plugin is applied.
 *
 * Third-party native runtimes the shim links against are declared with [prebuilt];
 * see [PrebuiltSpec].
 */
abstract class CBindingExtension {
    /** Directory containing the C headers to parse. */
    abstract val headersDir: DirectoryProperty

    /** Header names to `#include` in the generated JNI bridge. */
    abstract val includeHeaders: ListProperty<String>

    /** Package of the generated Kotlin bindings (drives the JNI symbol prefix). */
    abstract val jniPackage: Property<String>

    /** Base name of the generated Kotlin file. */
    abstract val kotlinFileName: Property<String>

    /** Output directory for the generated bridge C and Kotlin files. */
    abstract val outputDir: DirectoryProperty

    /** Third-party native runtimes, by name. Usually configured through [prebuilt]. */
    abstract val prebuilts: NamedDomainObjectContainer<PrebuiltSpec>

    /** Declares (or reconfigures) the prebuilt runtime [name]. */
    fun prebuilt(name: String, action: Action<PrebuiltSpec>): PrebuiltSpec =
        prebuilts.maybeCreate(name).also { action.execute(it) }
}
