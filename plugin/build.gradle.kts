plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // compileOnly: at runtime the consumer's own KGP is on the classpath; the
    // sourceSets API used here is stable across the versions we support.
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
    // compileOnly: prebuilt {} adds CMake arguments through the public AGP API; the
    // consumer's own AGP is on the classpath at runtime.
    compileOnly("com.android.tools.build:gradle-api:8.13.2")
    testImplementation(kotlin("test"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

group = "com.abyxcz.cbinding"
// -PlibVersion=X.Y.Z overrides (the tag-driven publish workflow passes it).
version = (findProperty("libVersion") as String?)?.takeIf { it.isNotBlank() } ?: "1.3.1"

gradlePlugin {
    plugins {
        create("cbindingPlugin") {
            id = "com.abyxcz.cbinding"
            implementationClass = "com.abyxcz.buildlogic.CBindingPlugin"
            displayName = "C-Binding KMP Automation Plugin"
            description = "Automates generation of JNI bridges and Kotlin bindings for C code in KMP projects."
        }
    }
}

publishing {
    repositories {
        mavenLocal()
        // Same credential chain as the ViewPoint libs: Actions token in CI,
        // GPR_USER/GPR_KEY or gpr.user/gpr.key (a PAT with write:packages) locally.
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/tjmtic/CBindingKMP")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: System.getenv("GPR_USER")
                    ?: findProperty("gpr.user") as String? ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: System.getenv("GPR_KEY")
                    ?: findProperty("gpr.key") as String? ?: ""
            }
        }
    }
}
