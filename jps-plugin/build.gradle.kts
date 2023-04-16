
fun properties(key: String) = providers.gradleProperty(key)

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
}

version = properties("pluginVersion").get()

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.bndlib) {
        exclude(group = "org.slf4j")
    }
}

kotlin {
    jvmToolchain(8)
}

intellij {
    version = properties("platformVersion")
    type = properties("platformType")
    plugins = listOf("com.intellij.java")
}

tasks {
    verifyPlugin {
        enabled = false
    }

    buildSearchableOptions {
        enabled = false
    }

    signPlugin {
        enabled = false
    }

    publishPlugin {
        enabled = false
    }

    runIde {
        enabled = false
    }
}
