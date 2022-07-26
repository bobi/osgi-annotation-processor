package com.github.bobi.osgiannotationprocessor.jps

import com.github.bobi.osgiannotationprocessor.settings.OSGIScrConfigurationImpl
import com.github.bobi.osgiannotationprocessor.settings.OSGIScrConfigurationProperties
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer

class ConfigurationSerializer : JpsProjectExtensionSerializer("osgi-scr.xml", "OsgiScrSettings") {
    override fun loadExtension(jpsProject: JpsProject, element: Element) {
        val properties: OSGIScrConfigurationProperties = XmlSerializer.deserialize(
            element, OSGIScrConfigurationProperties::class.java
        )

        JPSSCRExtensionService.setConfiguration(jpsProject, OSGIScrConfigurationImpl(properties))
    }
}