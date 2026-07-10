package com.abyxcz.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals

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
            CFunction("int", "add_numbers", listOf(CParam("int", "a"), CParam("int", "b"))),
            model.functions[0]
        )
        assertEquals(CFunction("void", "reset", emptyList()), model.functions[2])
    }

    @Test
    fun `parseAll merges headers in order`() {
        val model = CHeaderParser.parseAll(
            listOf("int first(int a);", "int second(int b);")
        )
        assertEquals(listOf("first", "second"), model.functions.map { it.name })
    }
}
