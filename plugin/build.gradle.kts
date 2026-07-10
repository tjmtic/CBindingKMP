plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // compileOnly: at runtime the consumer's own KGP is on the classpath; the
    // sourceSets API used here is stable across the versions we support.
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
    testImplementation(kotlin("test"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

group = "com.abyxcz.cbinding"
version = "1.1.0"

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
    }
}
