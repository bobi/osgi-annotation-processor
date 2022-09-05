package org.jetbrains.osgi.jps.com.github.bobi.osgiannotationprocessor.jps

import com.github.bobi.osgiannotationprocessor.jps.JPSSCRExtensionService
import com.github.bobi.osgiannotationprocessor.settings.OSGIScrConfigurationImpl
import com.github.bobi.osgiannotationprocessor.settings.OSGIScrConfigurationProperties
import org.jetbrains.jps.builders.JpsBuildTestCase
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.util.JpsPathUtil

abstract class OsgiBuildTestCase : JpsBuildTestCase() {
    fun module(name: String): JpsModule {
        val module = addModule(name)

        val contentRoot = JpsPathUtil.pathToUrl(getAbsolutePath(name))

        module.contentRootsList.addUrl(contentRoot)

        module.addSourceRoot("${contentRoot}/src", JavaSourceRootType.SOURCE)
        module.addSourceRoot("${contentRoot}/res", JavaResourceRootType.RESOURCE)

        JpsJavaExtensionService.getInstance().getOrCreateModuleExtension(module).outputUrl = "${contentRoot}/out"

        val configuration = OSGIScrConfigurationImpl(OSGIScrConfigurationProperties())

        configuration.enabled = true
        configuration.debugLogging = true

        JPSSCRExtensionService.setConfiguration(module.project, configuration)

        return module
    }

    fun extension() = JPSSCRExtensionService
}