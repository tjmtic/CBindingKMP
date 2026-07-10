package com.abyxcz.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
                    "    int result = add_numbers(a, b);\n" +
                    "    return (jint)result;\n" +
                    "}"
            ),
            bridge
        )
    }

    @Test
    fun `opaque handles cross as jlong with intptr_t casts`() {
        val model = CHeaderParser.parse(
            """
            typedef struct bd_ctx bd_ctx;
            bd_ctx* bd_open(int32_t mode);
            void bd_close(bd_ctx* ctx);
            """.trimIndent()
        )
        val bridge = JniCodegen.generate(model, config)

        assertTrue(bridge.contains("bd_ctx* result = bd_open((int32_t)mode);"), bridge)
        assertTrue(bridge.contains("return (jlong)(intptr_t)result;"), bridge)
        assertTrue(bridge.contains("bd_close((bd_ctx*)(intptr_t)ctx);"), bridge)
        assertTrue(bridge.contains("JNIEXPORT jlong JNICALL"), bridge)
    }

    @Test
    fun `const uint8_t pointer generates direct-ByteBuffer primary and array overload`() {
        val model = CHeaderParser.parse(
            """
            typedef struct bd_ctx bd_ctx;
            int32_t bd_feed(bd_ctx* ctx, const uint8_t* pixels, int32_t len);
            """.trimIndent()
        )
        val bridge = JniCodegen.generate(model, config)

        // Primary: direct ByteBuffer with null/non-direct rejection.
        assertTrue(bridge.contains("_bd_1feedJNI(JNIEnv *env, jclass clazz, jlong ctx, jobject pixels, jint len)"), bridge)
        assertTrue(bridge.contains("GetDirectBufferAddress(env, pixels)"), bridge)
        assertTrue(bridge.contains("must be a non-null direct ByteBuffer"), bridge)

        // Overload: byte array via Critical, released with JNI_ABORT (const input).
        assertTrue(bridge.contains("_bd_1feedArrJNI(JNIEnv *env, jclass clazz, jlong ctx, jbyteArray pixels, jint len)"), bridge)
        assertTrue(bridge.contains("GetPrimitiveArrayCritical(env, pixels, NULL)"), bridge)
        assertTrue(bridge.contains("ReleasePrimitiveArrayCritical(env, pixels, pixels_ptr, JNI_ABORT)"), bridge)
    }

    @Test
    fun `mutable float pointer uses Critical with write-back release`() {
        val model = CHeaderParser.parse("int32_t fill(float* out, int32_t max);")
        val bridge = JniCodegen.generate(model, config)

        assertTrue(bridge.contains("jfloatArray out"), bridge)
        assertTrue(bridge.contains("ReleasePrimitiveArrayCritical(env, out, out_ptr, 0)"), bridge)
        // Release happens BEFORE the return statement (native call result captured first).
        val releaseIdx = bridge.indexOf("ReleasePrimitiveArrayCritical(env, out")
        val returnIdx = bridge.indexOf("return (jint)result;")
        assertTrue(releaseIdx in 1 until returnIdx, bridge)
        // No array overload for functions without const uint8_t* params.
        assertFalse(bridge.contains("fillArrJNI"), bridge)
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
        val model = CHeaderParser.parse("int32_t bd_input_width(int32_t dummy);")
        val bridge = JniCodegen.generate(model, custom)

        assertTrue(bridge.contains("#include \"ball_detector.h\""), bridge)
        assertTrue(
            bridge.contains("Java_com_abyxcz_speedcamera_nn_generated_BallDetectorNativeKt_bd_1input_1widthJNI"),
            bridge
        )
    }
}
