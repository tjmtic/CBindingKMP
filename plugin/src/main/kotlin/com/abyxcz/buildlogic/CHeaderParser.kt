package com.abyxcz.buildlogic

/** The C type system subset the generator understands. */
sealed interface CType {
    /** `void`, `int`, `int32_t`, `int64_t`, `size_t`, `float`, `double`. */
    data class Scalar(val name: String) : CType

    /** Pointer to a primitive: `uint8_t*`, `float*`, `int32_t*` (optionally const). */
    data class Pointer(val pointee: String, val isConst: Boolean) : CType

    /** Pointer to an opaque struct declared via `typedef struct X X;`. */
    data class OpaqueHandle(val name: String, val isConst: Boolean) : CType

    /** `const char*`: a NUL-terminated UTF-8 input string. */
    object CString : CType {
        override fun toString() = "CString"
    }

    /**
     * `char*`: only valid as an out-string buffer, second to last and followed by an
     * `int32_t` capacity, in a function returning `int32_t`. See [CFunction.hasOutString].
     */
    object CharBuffer : CType {
        override fun toString() = "CharBuffer"
    }
}

data class CParam(val type: CType, val name: String)
data class CFunction(val returnType: CType, val name: String, val params: List<CParam>) {
    /** `int32_t f(..., char* out, int32_t cap)`: the caller-buffer out-string convention. */
    val hasOutString: Boolean
        get() = params.size >= 2 && params[params.size - 2].type == CType.CharBuffer

    /** Any string crossing the boundary: such functions get a Kotlin wrapper over a raw external. */
    val hasStrings: Boolean
        get() = hasOutString || params.any { it.type == CType.CString }
}
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
    // `char` itself is handled separately: `const char*` in, `char*` out-buffer.
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
            .map { it.substringBefore("//") }
            // Standard C++ compatibility guard: the preprocessor conditionals around it are
            // already stripped, so drop the guard itself and its closing brace line.
            .filterNot { it.trim() == "extern \"C\" {" || it.trim() == "}" }
            .joinToString("\n")
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
        if (returnType == CType.CString || returnType == CType.CharBuffer) {
            throw UnsupportedCDeclarationException(
                "String return types are not supported (who frees it?): '$decl;'. " +
                    "Write into a caller buffer instead: 'int32_t $name(..., char* out, int32_t cap);'"
            )
        }

        val params = parseParams(paramsRaw.trim(), opaqueTypes, decl)
        val function = CFunction(returnType, name, params)
        validateOutString(function, decl)
        return function
    }

    private fun validateOutString(function: CFunction, decl: String) {
        val buffers = function.params.count { it.type == CType.CharBuffer }
        if (buffers == 0) return
        val params = function.params
        val capacity = params.lastOrNull()?.type
        val returnsInt32 = (function.returnType as? CType.Scalar)?.name in setOf("int", "int32_t")
        val ok = buffers == 1 &&
            function.hasOutString &&
            (capacity as? CType.Scalar)?.name in setOf("int", "int32_t") &&
            returnsInt32
        if (!ok) {
            throw UnsupportedCDeclarationException(
                "A mutable 'char*' is only supported as the out-string buffer: second to last, " +
                    "followed by an int32_t capacity, in a function returning int32_t " +
                    "(bytes written, or -required capacity), e.g. " +
                    "'int32_t ${function.name}(..., char* out, int32_t cap);'. Got: '$decl;'"
            )
        }
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
            if (pointee == "char") return if (isConst) CType.CString else CType.CharBuffer
            if (pointee.removePrefix("const ").trim().startsWith("char")) {
                throw UnsupportedCDeclarationException(
                    "Unsupported string type '$raw' in '$decl;': only 'const char*' (input) and a " +
                        "'char*' out-string buffer are supported; arrays of strings are not"
                )
            }
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
