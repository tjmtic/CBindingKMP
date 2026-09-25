package com.abyxcz.buildlogic

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
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
    fun `extern C guard is tolerated`() {
        val model = CHeaderParser.parse(
            """
            #ifdef __cplusplus
            extern "C" {
            #endif

            int add_numbers(int a, int b);

            #ifdef __cplusplus
            }
            #endif
            """.trimIndent()
        )
        assertEquals(listOf("add_numbers"), model.functions.map { it.name })
    }

    @Test
    fun `void parameter list means no params`() {
        val model = CHeaderParser.parse("int get_answer(void);")
        assertEquals(emptyList(), model.functions.single().params)
    }

    @Test
    fun `unsupported constructs fail loudly`() {
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

    @Test
    fun `const char star is a UTF-8 input string`() {
        val f = CHeaderParser.parse("int32_t utf8_length(const char *text);").functions.single()
        assertEquals(CType.CString, f.params.single().type)
        assertTrue(f.hasStrings)
        assertFalse(f.hasOutString)
    }

    @Test
    fun `char star plus capacity at the end is the out-string convention`() {
        val f = CHeaderParser.parse(
            """
            typedef struct lm_ctx lm_ctx;
            int32_t lm_next_token(lm_ctx* ctx, char* out, int32_t cap);
            """.trimIndent()
        ).functions.single()
        assertEquals(listOf("ctx", "out", "cap"), f.params.map { it.name })
        assertEquals(CType.CharBuffer, f.params[1].type)
        assertTrue(f.hasOutString)
    }

    @Test
    fun `strings outside the convention are rejected with the reason`() {
        fun rejects(decl: String, fragment: String) {
            val e = assertFailsWith<UnsupportedCDeclarationException> { CHeaderParser.parse(decl) }
            assertTrue(e.message!!.contains(fragment), "'$decl' -> ${e.message}")
        }
        rejects("const char* version(void);", "String return types are not supported")
        rejects("char* dup(const char* s);", "String return types are not supported")
        rejects("int32_t join(const char** parts, int32_t n);", "arrays of strings are not")
        rejects("int32_t fill(char** out);", "arrays of strings are not")
        rejects("int32_t f(char* out, int32_t cap, int32_t flags);", "second to last")
        rejects("int32_t f(char* out);", "second to last")
        rejects("void f(const char* in, char* out, int32_t cap);", "returning int32_t")
        rejects("int32_t f(char* a, int32_t n, char* b, int32_t m);", "second to last")
        rejects("int32_t f(char* out, float cap);", "second to last")
    }
}
