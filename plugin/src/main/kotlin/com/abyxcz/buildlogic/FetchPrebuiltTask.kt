package com.abyxcz.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Fetches one prebuilt archive, verifies it, and extracts the useful part into [outputDir].
 *
 * The archive comes from [url] (sha256-pinned, cached by hash under [cacheDir] so a clean
 * build does not download again) or from [localArchive] (an AAR resolved from Maven).
 * With [xcframeworkName] set, only `<name>.xcframework` is kept and [requiredSlices] must
 * exist in it; otherwise the archive is extracted with a single top-level directory
 * stripped, filtered by [includes].
 */
abstract class FetchPrebuiltTask : DefaultTask() {

    @get:Optional @get:Input abstract val url: Property<String>

    @get:Optional @get:Input abstract val sha256: Property<String>

    @get:InputFiles @get:PathSensitive(PathSensitivity.NONE)
    abstract val localArchive: ConfigurableFileCollection

    @get:Input abstract val includes: ListProperty<String>

    @get:Optional @get:Input abstract val xcframeworkName: Property<String>

    @get:Input abstract val requiredSlices: ListProperty<String>

    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @get:Internal abstract val cacheDir: DirectoryProperty

    @get:Inject abstract val archives: ArchiveOperations

    @get:Inject abstract val fs: FileSystemOperations

    init {
        group = "cbinding"
        includes.convention(emptyList())
        requiredSlices.convention(emptyList())
    }

    @TaskAction
    fun fetch() {
        val archive = resolveArchive()
        val staging = File(temporaryDir, "extract")
        staging.deleteRecursively()
        staging.mkdirs()
        fs.copy {
            from(treeOf(archive))
            into(staging)
        }
        val root = xcframeworkName.orNull?.let { name ->
            PrebuiltArchives.findXcframework(staging, name).also {
                PrebuiltArchives.requireSlices(it, requiredSlices.get())
            }
        } ?: PrebuiltArchives.singleRootOrSelf(staging)
        val keep = includes.get()
        fs.sync {
            from(root)
            into(outputDir)
            if (keep.isNotEmpty()) include(keep)
        }
        staging.deleteRecursively()
    }

    private fun resolveArchive(): File {
        val local = localArchive.files
        if (local.isNotEmpty()) {
            check(local.size == 1) { "Expected one archive, got ${local.joinToString { it.name }}" }
            val file = local.single()
            sha256.orNull?.let { PrebuiltArchives.verify(file, it, file.name) }
            return file
        }
        val source = url.orNull ?: error("$name: neither a url nor a local archive is configured")
        val pin = sha256.orNull
            ?: error("$name: $source has no sha256. Remote archives must be pinned.")
        val cached = File(cacheDir.get().asFile, "${pin.lowercase()}/${PrebuiltArchives.fileNameOf(source)}")
        if (cached.exists()) {
            if (PrebuiltArchives.sha256(cached).equals(pin, ignoreCase = true)) return cached
            cached.delete()
        }
        logger.lifecycle("cbinding: downloading $source")
        PrebuiltArchives.download(source, cached)
        try {
            PrebuiltArchives.verify(cached, pin, source)
        } catch (e: IllegalStateException) {
            cached.delete()
            throw e
        }
        return cached
    }

    private fun treeOf(file: File): Any =
        when (PrebuiltArchives.kindOf(file.name)) {
            PrebuiltArchives.Kind.ZIP -> archives.zipTree(file)
            PrebuiltArchives.Kind.TAR_GZ -> archives.tarTree(archives.gzip(file))
            PrebuiltArchives.Kind.TAR -> archives.tarTree(file)
        }
}
