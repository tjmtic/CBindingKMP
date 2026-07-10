import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("com.abyxcz.cbinding")
    `maven-publish`
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
        // Compile the C sources into a static archive for this exact target, so the
        // cinterop klib carries real code (previously the demo C was never linked into
        // Kotlin/Native at all — .def had headers only).
        val sdk = if (target.name == "iosArm64") "iphoneos" else "iphonesimulator"
        val triple = if (target.name == "iosArm64") {
            "arm64-apple-ios12.0"
        } else {
            "arm64-apple-ios12.0-simulator"
        }
        val libDir = layout.buildDirectory.dir("native/${target.name}")
        val compileMylib = tasks.register<Exec>(
            "compileMylib${target.name.replaceFirstChar { it.uppercase() }}"
        ) {
            val srcFile = file("../native/c/mylib.c")
            val includeDir = file("../native/c")
            inputs.file(srcFile)
            inputs.dir(includeDir)
            outputs.dir(libDir)
            commandLine(
                "bash", "-c",
                "mkdir -p \"${libDir.get().asFile}\" && " +
                    "xcrun --sdk $sdk clang -target $triple -O2 -c \"$srcFile\" " +
                    "-I\"$includeDir\" -o \"${libDir.get().asFile}/mylib.o\" && " +
                    "ar rcs \"${libDir.get().asFile}/libmylib.a\" \"${libDir.get().asFile}/mylib.o\""
            )
        }

        target.compilations.getByName("main") {
            val mylib by cinterops.creating {
                defFile(project.file("src/nativeInterop/cinterop/mylib.def"))
                packageName("com.abyxcz.cbindingkmp.cinterop")
                includeDirs.headerFilterOnly(project.file("../native/c"))
                includeDirs(project.file("../native/c"))
                extraOpts("-libraryPath", libDir.get().asFile.absolutePath)
            }
        }
        tasks.named("cinteropMylib${target.name.replaceFirstChar { it.uppercase() }}") {
            dependsOn(compileMylib)
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


// Configure the C binding generator (task registration + source-set wiring are
// handled by the plugin).
cbinding {
    headersDir.set(file("../native/c"))
    includeHeaders.set(listOf("mylib.h"))
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications.withType<MavenPublication> {
        artifact(javadocJar)
        pom {
            name.set("C-Binding KMP Shared")
            description.set("Shared library with C-binding helpers")
            url.set("https://github.com/abyxcz/CBindingKMP")
            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("abyxcz")
                    name.set("Abyxcz")
                }
            }
            scm {
                url.set("https://github.com/abyxcz/CBindingKMP")
            }
        }
    }
    repositories {
        mavenLocal()
    }
}
