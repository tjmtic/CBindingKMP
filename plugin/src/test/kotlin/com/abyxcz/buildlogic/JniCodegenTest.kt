package com.abyxcz.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JniCodegenTest {

    private val config = CodegenConfig() // historical defaults

    @Test
    fun `jni symbol escapes underscores per the JNI spec`() {
        // Regression for the launch-blocking bug: `add_numbers` must produce
        // `add_1numbersJNI` in the symbol or the JVM throws UnsatisfiedLinkError.
        assertEquals(
            "Java_com_abyxcz_cbindingkmp_shared_generated_GeneratedNativeKt_add_1numbersJNI",
            JniCodegen.symbolFor(config, "add_numbersJNI")
        )
    }

    @Test
    fun `bridge for add_numbers matches expected shape`() {
        val model = CHeaderParser.parse("int add_numbers(int a, int b);")
        val bridge = JniCodegen.generate(model, config)

        assertTrue(bridge.contains("#include \"mylib.h\""), bridge)
        assertTrue(
            bridge.contains(
                "JNIEXPORT jint JNICALL\n" +
                    "Java_com_abyxcz_cbindingkmp_shared_generated_GeneratedNativeKt_add_1numbersJNI" +
                    "(JNIEnv *env, jclass clazz, jint a, jint b) {\n" +
                    "    return add_numbers(a, b);\n" +
                    "}"
            ),
            bridge
        )
    }

    @Test
    fun `void functions do not emit return`() {
        val model = CHeaderParser.parse("void reset();")
        val bridge = JniCodegen.generate(model, config)
        assertTrue(bridge.contains("    reset();\n"), bridge)
    }

    @Test
    fun `configurable package and includes are honored`() {
        val custom = CodegenConfig(
            includeHeaders = listOf("ball_detector.h"),
            jniPackage = "com.abyxcz.speedcamera.nn.generated",
            kotlinFileName = "BallDetectorNative"
        )
        val model = CHeaderParser.parse("int bd_input_width(int dummy);")
        val bridge = JniCodegen.generate(model, custom)

        assertTrue(bridge.contains("#include \"ball_detector.h\""), bridge)
        assertTrue(
            bridge.contains("Java_com_abyxcz_speedcamera_nn_generated_BallDetectorNativeKt_bd_1input_1widthJNI"),
            bridge
        )
    }
}
