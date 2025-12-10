import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    val nativeTargets = listOf(
        iosArm64(),
        iosSimulatorArm64()
    )

    nativeTargets.forEach { target ->
        target.compilations.getByName("main") {
            val mylib by cinterops.creating {
                defFile(project.file("src/nativeInterop/cinterop/mylib.def"))
                packageName("com.abyxcz.cbindingkmp.cinterop")
                includeDirs.headerFilterOnly(project.file("../native/c"))
                includeDirs(project.file("../native/c"))
            }
        }
    }
    
    jvm()
    
    js {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    sourceSets {
        commonMain.dependencies {
            // put your Multiplatform dependencies here
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.abyxcz.cbindingkmp.shared"
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


val generateJni by tasks.registering(com.abyxcz.buildlogic.JniGeneratorTask::class) {
    inputDir.set(file("../native/c"))
    outputDir.set(layout.buildDirectory.dir("generated/jni"))
}

// Make sure preBuild depends on it so files exist
tasks.named("preBuild").configure {
    dependsOn(generateJni)
}

// Add generated Kotlin code to source sets
kotlin {
    sourceSets {
        androidMain {
            kotlin.srcDir(generateJni.map { it.outputDir })
        }
    }
}
