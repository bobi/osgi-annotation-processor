import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java") // Java support
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

dependencies {
    testImplementation(libs.bundles.test)

    implementation(libs.bundles.bndlib) {
        exclude(group = "org.slf4j")
    }

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        testFramework(TestFrameworkType.Platform)
    }
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") {
    compilerOptions.jvmTarget.set(
        providers.gradleProperty("jpsJavaTargetVersion")
            .map { JvmTarget.fromTarget(JavaVersion.toVersion(it).toString()) }
    )
}

tasks.named<JavaCompile>("compileJava") {
    options.release = providers.gradleProperty("jpsJavaTargetVersion")
        .map { JavaVersion.toVersion(it).majorVersion.toInt() }
}

intellijPlatform {
}

tasks {
    prepareJarSearchableOptions {
        enabled = false
    }

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
