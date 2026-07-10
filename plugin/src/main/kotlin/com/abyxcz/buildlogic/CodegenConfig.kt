package com.abyxcz.buildlogic

/**
 * Configuration shared by the JNI and Kotlin code generators. Defaults preserve the
 * historical hardcoded behavior; the Gradle extension surfaces these to consumers.
 */
data class CodegenConfig(
    /** Header file names to `#include` in the generated JNI bridge. */
    val includeHeaders: List<String> = listOf("mylib.h"),
    /** Package of the generated Kotlin file (also determines the JNI symbol prefix). */
    val jniPackage: String = "com.abyxcz.cbindingkmp.shared.generated",
    /** Base name of the generated Kotlin file (class in JNI terms is `<name>Kt`). */
    val kotlinFileName: String = "GeneratedNative"
) {
    /** The JNI "class" name the JVM derives for top-level functions in the Kotlin file. */
    val jniClassName: String get() = "${kotlinFileName}Kt"
}
