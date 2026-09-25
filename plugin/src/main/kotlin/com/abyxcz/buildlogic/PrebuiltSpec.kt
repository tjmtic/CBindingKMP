package com.abyxcz.buildlogic

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Nested

/**
 * One third-party native runtime that a C shim links against, declared once and wired on
 * every platform:
 *
 * ```kotlin
 * cbinding {
 *     prebuilt("tflite") {
 *         ios { xcframework(url = "https://…/TensorFlowLiteC-2.17.0.tar.gz", sha256 = "…", name = "TensorFlowLiteC") }
 *         android { aar("com.google.ai.edge.litert:litert:1.4.2") }
 *     }
 * }
 * ```
 *
 * The plugin registers `fetchPrebuilt<Name>Ios` / `fetchPrebuilt<Name>Android`, fetches and
 * sha256-verifies each archive once (cached under the Gradle user home), and wires the result:
 * cinterop include dirs, `-F`/`-framework` linker flags and task dependencies on iOS; a
 * `-D<VAR>=<dir>` CMake argument and a `preBuild` dependency on Android.
 */
abstract class PrebuiltSpec : Named {
    @get:Nested abstract val ios: IosPrebuiltSpec

    @get:Nested abstract val android: AndroidPrebuiltSpec

    fun ios(action: Action<IosPrebuiltSpec>) = action.execute(ios)

    fun android(action: Action<AndroidPrebuiltSpec>) = action.execute(android)

    /** `tflite` → `Tflite`, `llama-cpp` → `LlamaCpp`. */
    internal val taskSuffix: String
        get() =
            name.split('-', '_', ' ', '.').filter { it.isNotEmpty() }.joinToString("") {
                it.replaceFirstChar(Char::uppercase)
            }

    val iosFetchTaskName: String
        get() = "fetchPrebuilt${taskSuffix}Ios"

    val androidFetchTaskName: String
        get() = "fetchPrebuilt${taskSuffix}Android"
}

/** An `.xcframework` inside a remote archive (`.zip`, `.tar.gz`, `.tgz`, `.tar`). */
abstract class IosPrebuiltSpec {
    abstract val url: Property<String>

    abstract val sha256: Property<String>

    /** Framework name: `TensorFlowLiteC` for `TensorFlowLiteC.xcframework`. Drives `-framework`. */
    abstract val frameworkName: Property<String>

    /**
     * Directory that receives `<frameworkName>.xcframework`. Defaults to
     * `build/cbinding/prebuilt/<name>/ios`; point it at a source directory when an Xcode
     * project references the framework by path.
     */
    abstract val destination: DirectoryProperty

    /** Slice linked by `iosArm64`. */
    abstract val deviceSlice: Property<String>

    /** Slice linked by `iosSimulatorArm64` / `iosX64`. */
    abstract val simulatorSlice: Property<String>

    /** Extra flags after `-framework <name>` on every linked iOS binary. Default `-lc++`. */
    abstract val linkerOpts: ListProperty<String>

    fun xcframework(url: String, sha256: String, name: String) {
        this.url.set(url)
        this.sha256.set(sha256)
        frameworkName.set(name)
    }

    fun into(dir: Any) {
        when (dir) {
            is java.io.File -> destination.set(dir)
            is Directory -> destination.set(dir)
            else -> error("into() takes a File or Directory, got ${dir::class.java.name}")
        }
    }

    val isConfigured: Boolean
        get() = url.isPresent

    fun xcframeworkDir(): Provider<Directory> =
        destination.zip(frameworkName) { dir, name -> dir.dir("$name.xcframework") }

    fun sliceDir(device: Boolean): Provider<Directory> =
        xcframeworkDir().zip(if (device) deviceSlice else simulatorSlice) { xcf, slice ->
            xcf.dir(slice)
        }

    /** Headers of the framework in one slice — for a consumer's own shim compile step. */
    fun headersDir(device: Boolean): Provider<Directory> =
        sliceDir(device).zip(frameworkName) { slice, name -> slice.dir("$name.framework/Headers") }
}

/** Android side: an AAR from a Maven repository, or a remote archive (e.g. a source tarball). */
abstract class AndroidPrebuiltSpec {
    /** `group:artifact:version` of an AAR, resolved from the project's repositories. */
    abstract val aarCoordinates: Property<String>

    abstract val url: Property<String>

    abstract val sha256: Property<String>

    /** Ant-style patterns kept from the archive; empty keeps everything. */
    abstract val includes: ListProperty<String>

    /** Directory the archive is extracted into; handed to CMake as [cmakeVariable]. */
    abstract val destination: DirectoryProperty

    /** CMake variable carrying [destination]. Default `CBINDING_<NAME>_DIR`. */
    abstract val cmakeVariable: Property<String>

    /** An AAR's C headers and JNI libraries: its `headers` and `jni` trees unless [include] is given. */
    fun aar(coordinates: String, vararg include: String) {
        aarCoordinates.set(coordinates)
        includes.set(if (include.isEmpty()) listOf("headers/**", "jni/**") else include.toList())
    }

    /** A remote archive; a single top-level directory is stripped. */
    fun archive(url: String, sha256: String, vararg include: String) {
        this.url.set(url)
        this.sha256.set(sha256)
        includes.set(include.toList())
    }

    val isConfigured: Boolean
        get() = aarCoordinates.isPresent || url.isPresent
}
