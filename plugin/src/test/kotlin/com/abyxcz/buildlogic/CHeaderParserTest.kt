package com.abyxcz.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CHeaderParserTest {

    @Test
    fun `parses scalar declarations`() {
        val model = CHeaderParser.parse(
            """
            #ifndef MYLIB_H
            #define MYLIB_H
            int add_numbers(int a, int b);
            float scale(float value, float factor);
            void reset();
            double half(double x);
            #endif
            """.trimIndent()
        )

        assertEquals(4, model.functions.size)
        assertEquals(
            CFunction(
                CType.Scalar("int"), "add_numbers",
                listOf(CParam(CType.Scalar("int"), "a"), CParam(CType.Scalar("int"), "b"))
            ),
            model.functions[0]
        )
        assertEquals(CFunction(CType.Scalar("void"), "reset", emptyList()), model.functions[2])
    }

    @Test
    fun `parses the ball_detector shape - opaque handles, pointers, multi-line`() {
        val model = CHeaderParser.parse(
            """
            /* Opaque context. */
            typedef struct bd_ctx bd_ctx;

            bd_ctx* bd_create(const uint8_t* model_bytes, int32_t model_len, int32_t num_threads);

            int32_t bd_detect(bd_ctx* ctx, const uint8_t* pixels,
                              int32_t width, int32_t height, int32_t stride_bytes,
                              int32_t pixel_format, int32_t rotation, float score_threshold,
                              float* out_detections, int32_t max_out);

            void bd_destroy(bd_ctx* ctx); // frees everything
            """.trimIndent()
        )

        assertEquals(setOf("bd_ctx"), model.opaqueTypes)
        assertEquals(3, model.functions.size)

        val create = model.functions[0]
        assertEquals(CType.OpaqueHandle("bd_ctx", isConst = false), create.returnType)
        assertEquals(CType.Pointer("uint8_t", isConst = true), create.params[0].type)

        val detect = model.functions[1]
        assertEquals(10, detect.params.size)
        assertEquals(CType.OpaqueHandle("bd_ctx", isConst = false), detect.params[0].type)
        assertEquals(CType.Pointer("float", isConst = false), detect.params[8].type)
        assertEquals("out_detections", detect.params[8].name)
    }

    @Test
    fun `comments and preprocessor lines are stripped`() {
        val model = CHeaderParser.parse(
            """
            // int not_a_function(int x);
            /* int also_not(int x); */
            #define IGNORED int fake(int x);
            int real(int x);
            """.trimIndent()
        )
        assertEquals(listOf("real"), model.functions.map { it.name })
    }

    @Test
    fun `void parameter list means no params`() {
        val model = CHeaderParser.parse("int get_answer(void);")
        assertEquals(emptyList(), model.functions.single().params)
    }

    @Test
    fun `unsupported constructs fail loudly`() {
        assertFailsWith<UnsupportedCDeclarationException> {
            CHeaderParser.parse("int takes_string(const char* s);")
        }
        assertFailsWith<UnsupportedCDeclarationException> {
            CHeaderParser.parse("struct Point make_point(int x, int y);")
        }
        assertFailsWith<UnsupportedCDeclarationException> {
            CHeaderParser.parse("float* returns_buffer(int n);")
        }
        assertFailsWith<UnsupportedCDeclarationException> {
            CHeaderParser.parse("int unknown_handle(widget* w);")
        }
    }

    @Test
    fun `parseAll merges headers and opaque registry in order`() {
        val model = CHeaderParser.parseAll(
            listOf(
                "typedef struct ctx ctx;\nctx* open_it(void);",
                "void close_it(ctx* c);"
            )
        )
        assertEquals(listOf("open_it", "close_it"), model.functions.map { it.name })
        assertEquals(CType.OpaqueHandle("ctx", isConst = false), model.functions[1].params[0].type)
    }
}
