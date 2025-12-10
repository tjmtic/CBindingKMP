import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("com.abyxcz.cbinding") version "1.0.0"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    jvm()

    sourceSets {
        commonMain.dependencies {
            // put your Multiplatform dependencies here
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain {
            // Include generated code
            kotlin.srcDir(tasks.named("generateJni", com.abyxcz.buildlogic.JniGeneratorTask::class).map { it.outputDir })
        }
        jvmMain {
            kotlin.srcDir(tasks.named("generateJni", com.abyxcz.buildlogic.JniGeneratorTask::class).map { it.outputDir })
        }
    }
}

android {
    namespace = "com.abyxcz.sampleapplication.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        externalNativeBuild {
            cmake {
                cppFlags("")
            }
        }
    }
    externalNativeBuild {
        cmake {
            path = file("../native/c/CMakeLists.txt")
        }
    }
}

// Configure the task
tasks.named("generateJni", com.abyxcz.buildlogic.JniGeneratorTask::class) {
    inputDir.set(file("../native/c"))
}
