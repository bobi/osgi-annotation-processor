package com.github.bobi.osgiannotationprocessor.settings

import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.jps.model.ex.JpsElementBase

class OSGIScrConfigurationImpl(val properties: OSGIScrConfigurationProperties = OSGIScrConfigurationProperties()) :
    JpsElementBase<OSGIScrConfigurationImpl?>(), OSGIScrConfiguration by properties {

    override fun createCopy(): OSGIScrConfigurationImpl =
        OSGIScrConfigurationImpl(XmlSerializerUtil.createCopy(properties))

    override fun applyChanges(configuration: OSGIScrConfigurationImpl) {
        XmlSerializerUtil.copyBean(configuration.properties, properties)
    }
}