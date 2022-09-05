import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

tasks {
    jar {
        archiveFileName.set("osgi-jps-plugin.jar")
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

    project.findProperty("jpsJavaTargetVersion").toString().let {
        withType<JavaCompile> {
            targetCompatibility = it
        }
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = it
        }
    }
}

dependencies {
    implementation("org.apache.felix:org.apache.felix.scr.bnd:1.9.6")
    implementation("biz.aQute.bnd:biz.aQute.bndlib:5.3.0") {
        exclude("org.slf4j")
    }
}

