tasks {
    jar {
        archiveFileName.set("osgi-jps-plugin.jar")
    }
}

dependencies {
    implementation("org.apache.felix:org.apache.felix.scr.bnd:1.9.6")
    implementation("biz.aQute.bnd:biz.aQute.bndlib:5.1.0")
}

