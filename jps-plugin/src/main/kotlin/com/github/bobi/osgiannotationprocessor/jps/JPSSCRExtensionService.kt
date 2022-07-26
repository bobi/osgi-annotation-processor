package com.github.bobi.osgiannotationprocessor.jps

import com.github.bobi.osgiannotationprocessor.settings.OSGIScrConfiguration
import com.github.bobi.osgiannotationprocessor.settings.OSGIScrConfigurationImpl
import org.jetbrains.jps.model.JpsProject

object JPSSCRExtensionService {
    fun getConfiguration(project: JpsProject): OSGIScrConfiguration {
        val config: OSGIScrConfigurationImpl? = project.container.getChild(OSGIScrConfiguration.ROLE)

        return config ?: OSGIScrConfigurationImpl()
    }

    fun setConfiguration(project: JpsProject, configuration: OSGIScrConfigurationImpl?): OSGIScrConfiguration {
        return project.container.setChild(OSGIScrConfiguration.ROLE, configuration)
    }
}