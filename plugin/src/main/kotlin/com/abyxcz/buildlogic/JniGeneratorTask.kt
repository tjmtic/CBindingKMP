package com.abyxcz.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class JniGeneratorTask : DefaultTask() {

    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val input = inputDir.get().asFile
        val output = outputDir.get().asFile
        
        // Ensure output dir exists
        output.mkdirs()
        
        val headerFiles = input.listFiles { file -> file.name.endsWith(".h") } ?: return
        
        val functions = mutableListOf<FunctionDef>()
        
        headerFiles.forEach { header ->
            val content = header.readText()
            // Regex to find simple function declarations: ReturnType functionName(ArgType argName, ...)
            // Limitation: Very simple parser, assumes 1 line decl, no complex types.
            // Matches: int add_numbers(int a, int b);
            val regex = Regex("""\b(int|void|float|double)\s+(\w+)\s*\(([^)]*)\);""")
            
            regex.findAll(content).forEach { match ->
                 val returnType = match.groupValues[1]
                 val name = match.groupValues[2]
                 val args = match.groupValues[3]
                 functions.add(FunctionDef(returnType, name, args))
            }
        }
        
        generateJniBridge(functions, output)
        generateKotlinObject(functions, output)
    }
    
    data class FunctionDef(val returnType: String, val name: String, val args: String)

    private fun generateJniBridge(functions: List<FunctionDef>, output: File) {
         val sb = StringBuilder()
         sb.append("#include <jni.h>\n")
         sb.append("#include \"mylib.h\"\n\n") // In real code, we might need to know which header to include
         
         functions.forEach { func ->
             // JNI Name: Java_package_class_method
             // Package: com.abyxcz.cbindingkmp.shared.generated
             // Class: GeneratedNative
             val jniName = "Java_com_abyxcz_cbindingkmp_shared_generated_GeneratedNativeKt_${func.name}JNI"
             
             // Convert C args to JNI args
             var jniArgs = "JNIEnv *env, jclass clazz"
             val callArgs = mutableListOf<String>()
             
             if (func.args.isNotBlank()) {
                 func.args.split(",").forEach { arg ->
                     val parts = arg.trim().split(Regex("\\s+"))
                     if (parts.size >= 2) {
                         val type = parts[0]
                         val name = parts[1]
                         val jniType = toJniType(type)
                         jniArgs += ", $jniType $name"
                         callArgs.add(name)
                     }
                 }
             }
             
             val jniReturnType = toJniType(func.returnType)
             
             sb.append("JNIEXPORT $jniReturnType JNICALL\n")
             sb.append("$jniName($jniArgs) {\n")
             sb.append("    return ${func.name}(${callArgs.joinToString(", ")});\n")
             sb.append("}\n\n")
         }
         
         File(output, "jni_gen_bridge.c").writeText(sb.toString())
    }
    
    private fun generateKotlinObject(functions: List<FunctionDef>, output: File) {
        val sb = StringBuilder()
        sb.append("package com.abyxcz.cbindingkmp.shared.generated\n\n")
        
        functions.forEach { func ->
             // external fun
             val kotlinName = func.name + "JNI"
             
             var kotlinArgs = ""
             if (func.args.isNotBlank()) {
                 kotlinArgs = func.args.split(",").map { arg ->
                     val parts = arg.trim().split(Regex("\\s+"))
                     if (parts.size >= 2) {
                         val name = parts[1]
                         "${name}: ${toKotlinType(parts[0])}"
                     } else ""
                 }.joinToString(", ")
             }
             
             val kotlinReturn = toKotlinType(func.returnType)
             
             sb.append("internal external fun $kotlinName($kotlinArgs): $kotlinReturn\n")
        }
        
        File(output, "GeneratedNative.kt").writeText(sb.toString())
    }
    
    private fun toJniType(cType: String): String {
        return when(cType) {
            "int" -> "jint"
            "void" -> "void"
            "float" -> "jfloat"
            "double" -> "jdouble"
            else -> "jobject"
        }
    }
    
    private fun toKotlinType(cType: String): String {
        return when(cType) {
            "int" -> "Int"
            "void" -> "Unit"
            "float" -> "Float"
            "double" -> "Double"
            else -> "Any" 
        }
    }
}
