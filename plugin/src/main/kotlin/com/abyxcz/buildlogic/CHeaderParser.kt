package com.abyxcz.buildlogic

/** A parsed C function parameter. */
data class CParam(val type: String, val name: String)

/** A parsed C function declaration. */
data class CFunction(val returnType: String, val name: String, val params: List<CParam>)

/** The parse result for one or more headers. */
data class CHeaderModel(val functions: List<CFunction>)

/**
 * Parser for the supported C declaration subset. Pure (no Gradle types) so it is
 * unit-testable in isolation.
 *
 * Current subset: single-line declarations of scalar functions —
 * `int|void|float|double name(type name, ...);`
 * (extended to pointers/opaque handles in the marshalling milestone).
 */
object CHeaderParser {

    private val declRegex = Regex("""\b(int|void|float|double)\s+(\w+)\s*\(([^)]*)\);""")

    fun parse(content: String): CHeaderModel {
        val functions = declRegex.findAll(content).map { match ->
            CFunction(
                returnType = match.groupValues[1],
                name = match.groupValues[2],
                params = parseParams(match.groupValues[3])
            )
        }.toList()
        return CHeaderModel(functions)
    }

    fun parseAll(contents: List<String>): CHeaderModel =
        CHeaderModel(contents.flatMap { parse(it).functions })

    private fun parseParams(raw: String): List<CParam> {
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { arg ->
            val parts = arg.trim().split(Regex("\\s+"))
            if (parts.size >= 2) CParam(type = parts.dropLast(1).joinToString(" "), name = parts.last())
            else null
        }
    }
}
