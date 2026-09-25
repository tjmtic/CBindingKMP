package com.abyxcz.buildlogic

import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

/**
 * Pure helpers behind [FetchPrebuiltTask]: download, verify, and locate things inside an
 * extracted archive. No Gradle types, so they are unit-tested directly.
 */
object PrebuiltArchives {

    enum class Kind { ZIP, TAR_GZ, TAR }

    fun kindOf(fileName: String): Kind {
        val n = fileName.lowercase()
        return when {
            n.endsWith(".zip") || n.endsWith(".aar") || n.endsWith(".jar") -> Kind.ZIP
            n.endsWith(".tar.gz") || n.endsWith(".tgz") -> Kind.TAR_GZ
            n.endsWith(".tar") -> Kind.TAR
            else -> throw IllegalArgumentException(
                "Unsupported archive '$fileName': expected .zip, .aar, .tar.gz, .tgz or .tar"
            )
        }
    }

    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** Throws with both hashes when [file] does not hash to [expected]. */
    fun verify(file: File, expected: String, source: String) {
        val actual = sha256(file)
        check(actual.equals(expected.trim(), ignoreCase = true)) {
            "sha256 mismatch for $source\n  expected: ${expected.trim().lowercase()}\n  actual:   $actual\n" +
                "The archive changed upstream or the pin is wrong. Nothing was extracted."
        }
    }

    /** Last path segment of a URL, without query string: the cache file name. */
    fun fileNameOf(url: String): String {
        val path = URI(url).path ?: ""
        val name = path.substringAfterLast('/')
        require(name.isNotEmpty()) { "Cannot derive a file name from URL '$url'" }
        return name
    }

    /**
     * Downloads [url] to [dest] (via a `.part` file, so an interrupted download never looks
     * complete). Follows redirects, including cross-host ones (GitHub release assets).
     */
    fun download(url: String, dest: File, attempts: Int = 3) {
        dest.parentFile.mkdirs()
        val part = File(dest.parentFile, dest.name + ".part")
        var last: Exception? = null
        repeat(attempts) { attempt ->
            try {
                openFollowingRedirects(url).use { input ->
                    part.outputStream().use { out -> input.copyTo(out, 1 shl 16) }
                }
                if (dest.exists()) dest.delete()
                check(part.renameTo(dest)) { "Could not move ${part.name} into place" }
                return
            } catch (e: Exception) {
                last = e
                part.delete()
                if (attempt < attempts - 1) Thread.sleep(1000L * (attempt + 1))
            }
        }
        throw IllegalStateException("Download failed after $attempts attempts: $url", last)
    }

    private fun openFollowingRedirects(url: String, hops: Int = 5): java.io.InputStream {
        val conn = URI(url).toURL().openConnection()
        if (conn !is HttpURLConnection) return conn.getInputStream() // file:// in tests
        conn.instanceFollowRedirects = false
        conn.connectTimeout = 30_000
        conn.readTimeout = 120_000
        val code = conn.responseCode
        if (code in 300..399) {
            val location = conn.getHeaderField("Location")
            conn.disconnect()
            check(hops > 0 && location != null) { "Too many redirects fetching $url" }
            return openFollowingRedirects(URI(url).resolve(location).toString(), hops - 1)
        }
        check(code == 200) { "HTTP $code fetching $url" }
        return conn.inputStream
    }

    /** The single directory named `<name>.xcframework` anywhere under [root]. */
    fun findXcframework(root: File, name: String): File {
        val want = "$name.xcframework"
        val found = root.walkTopDown()
            .onEnter { !it.name.endsWith(".xcframework") || it == root || it.name == want }
            .filter { it.isDirectory && it.name == want }
            .toList()
        val others = root.walkTopDown().filter { it.isDirectory && it.name.endsWith(".xcframework") }
            .map { it.name }.distinct().toList()
        return when (found.size) {
            1 -> found.single()
            0 -> error(
                "No $want in the archive. " +
                    if (others.isEmpty()) "It contains no .xcframework at all."
                    else "It contains: ${others.joinToString()} — set name to one of those."
            )
            else -> error("Found ${found.size} copies of $want in the archive: ${found.joinToString { it.path }}")
        }
    }

    /** Fails listing the slices that do exist when any of [slices] is missing. */
    fun requireSlices(xcframework: File, slices: List<String>) {
        val present = xcframework.listFiles { f -> f.isDirectory }?.map { it.name }?.sorted() ?: emptyList()
        val missing = slices.filter { it !in present }
        check(missing.isEmpty()) {
            "${xcframework.name} has no slice ${missing.joinToString()}; it has ${present.joinToString()}. " +
                "Set deviceSlice / simulatorSlice to match."
        }
    }

    /** [dir]'s only child when it holds exactly one directory and nothing else, else [dir]. */
    fun singleRootOrSelf(dir: File): File {
        val children = dir.listFiles()?.filter { it.name != ".DS_Store" } ?: return dir
        return if (children.size == 1 && children[0].isDirectory) children[0] else dir
    }
}
