package com.abyxcz.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class CBindingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Register the task
        val generateJni = project.tasks.register("generateJni", JniGeneratorTask::class.java) {
             this.inputDir.set(project.file("native/c")) // Default convention
             this.outputDir.set(project.layout.buildDirectory.dir("generated/jni"))
        }
        
        // Ensure preBuild depends on it (for Android)
        // Ensure preBuild depends on it (for Android)
        project.tasks.configureEach {
            if (name == "preBuild") {
                dependsOn(generateJni)
            }
        }
        
        // We can't automatically add to sourceSets here easily without knowing the plugins applied (Android vs JVM)
        // For now, we leave the sourceSet wiring to the consumer or try to detect.
        // But to keep it robust as a library, we should probably expose an extension.
        // For simplicity of this "demo" transformation, we will just register the task and let the user wire it,
        // OR we can try to react to the Android plugin.
        
        project.afterEvaluate {
            val androidExtension = project.extensions.findByName("android")
            if (androidExtension != null) {
                // If Android plugin is present, try to add source set
                // Reflective access or just simple convention if we assume kotlin-android
                // This is a bit complex to do 100% correctly generically without deps, 
                // but since we are automating:
                
                // Let's print a message that it's applied
                println("CBindingPlugin applied. Task 'generateJni' is available.")
            }
        }
    }
}
