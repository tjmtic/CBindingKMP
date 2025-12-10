plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

group = "com.abyxcz.cbinding"
version = "1.0.0"

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
