package com.github.bobi.osgiannotationprocessor.jps

import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer

class ModelSerializerExtension : JpsModelSerializerExtension() {
    override fun getProjectExtensionSerializers(): MutableList<out JpsProjectExtensionSerializer> {
        return mutableListOf(ConfigurationSerializer())
    }
}