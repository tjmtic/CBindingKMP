package com.abyxcz.buildlogic

/** The C type system subset the generator understands. */
sealed interface CType {
    /** `void`, `int`, `int32_t`, `int64_t`, `size_t`, `float`, `double`. */
    data class Scalar(val name: String) : CType

    /** Pointer to a primitive: `uint8_t*`, `float*`, `int32_t*` (optionally const). */
    data class Pointer(val pointee: String, val isConst: Boolean) : CType

    /** Pointer to an opaque struct declared via `typedef struct X X;`. */
    data class OpaqueHandle(val name: String, val isConst: Boolean) : CType
}

data class CParam(val type: CType, val name: String)
data class CFunction(val returnType: CType, val name: String, val params: List<CParam>)
data class CHeaderModel(val functions: List<CFunction>, val opaqueTypes: Set<String>)

/** Thrown when a header declares something outside the supported subset. */
class UnsupportedCDeclarationException(message: String) : IllegalArgumentException(message)

/**
 * Parser for the supported C declaration subset (see docs/supported-c-subset.md):
 * comments and preprocessor lines are stripped; declarations may span lines;
 * `typedef struct X X;` registers an opaque handle; functions may use the scalar
 * types above plus primitive pointers and opaque-handle pointers.
 *
 * Anything else fails loudly with the offending declaration text — silent fallback
 * to `jobject`/`Any` produced unusable bindings in the original generator.
 */
object CHeaderParser {

    private val scalarTypes = setOf("void", "int", "int32_t", "int64_t", "size_t", "float", "double")
    private val pointerPointees = setOf("uint8_t", "float", "int32_t")
    private val typedefOpaqueRegex = Regex("""^typedef\s+struct\s+(\w+)\s+(\w+)$""")

    fun parse(content: String): CHeaderModel = parseAll(listOf(content))

    fun parseAll(contents: List<String>): CHeaderModel {
        val opaqueTypes = mutableSetOf<String>()
        val functions = mutableListOf<CFunction>()

        contents.forEach { raw ->
            val cleaned = stripCommentsAndPreprocessor(raw)
            // Declarations end at ';'. Multi-line decls collapse naturally.
            cleaned.split(';').map { it.trim().replace(Regex("\\s+"), " ") }
                .filter { it.isNotBlank() }
                .forEach { decl ->
                    val typedef = typedefOpaqueRegex.matchEntire(decl)
                    if (typedef != null) {
                        val (structName, aliasName) = typedef.destructured
                        if (structName != aliasName) {
                            throw UnsupportedCDeclarationException(
                                "Only self-aliasing opaque typedefs are supported: '$decl;'"
                            )
                        }
                        opaqueTypes.add(aliasName)
                        return@forEach
                    }
                    functions.add(parseFunction(decl, opaqueTypes))
                }
        }

        return CHeaderModel(functions, opaqueTypes)
    }

    private fun stripCommentsAndPreprocessor(content: String): String {
        val noBlock = content.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ")
        return noBlock.lines()
            .filterNot { it.trimStart().startsWith("#") }
            .joinToString("\n") { it.substringBefore("//") }
    }

    private fun parseFunction(decl: String, opaqueTypes: Set<String>): CFunction {
        val match = Regex("""^(.+?)\s*\b(\w+)\s*\((.*)\)$""").matchEntire(decl)
            ?: throw UnsupportedCDeclarationException("Cannot parse declaration: '$decl;'")
        val (returnRaw, name, paramsRaw) = match.destructured

        val returnType = parseType(returnRaw.trim(), opaqueTypes, decl)
        if (returnType is CType.Pointer) {
            throw UnsupportedCDeclarationException(
                "Primitive-pointer return types are not supported: '$decl;'"
            )
        }

        val params = parseParams(paramsRaw.trim(), opaqueTypes, decl)
        return CFunction(returnType, name, params)
    }

    private fun parseParams(raw: String, opaqueTypes: Set<String>, decl: String): List<CParam> {
        if (raw.isBlank() || raw == "void") return emptyList()
        return raw.split(',').map { arg ->
            val trimmed = arg.trim()
            val nameMatch = Regex("""^(.+?)\s*\b(\w+)$""").matchEntire(trimmed)
                ?: throw UnsupportedCDeclarationException("Cannot parse parameter '$trimmed' in '$decl;'")
            val (typeRaw, name) = nameMatch.destructured
            CParam(parseType(typeRaw.trim(), opaqueTypes, decl), name)
        }
    }

    private fun parseType(raw: String, opaqueTypes: Set<String>, decl: String): CType {
        val isConst = raw.startsWith("const ")
        val base = raw.removePrefix("const ").trim()

        if (base.endsWith("*")) {
            val pointee = base.dropLast(1).trim()
            return when {
                pointee in pointerPointees -> CType.Pointer(pointee, isConst)
                pointee in opaqueTypes -> CType.OpaqueHandle(pointee, isConst)
                else -> throw UnsupportedCDeclarationException(
                    "Unsupported pointer type '$raw' in '$decl;' " +
                        "(supported pointees: $pointerPointees or opaque typedefs)"
                )
            }
        }

        if (isConst) {
            throw UnsupportedCDeclarationException("const scalars are not supported: '$raw' in '$decl;'")
        }
        if (base in scalarTypes) return CType.Scalar(base)

        throw UnsupportedCDeclarationException(
            "Unsupported type '$raw' in '$decl;' (supported scalars: $scalarTypes)"
        )
    }
}
