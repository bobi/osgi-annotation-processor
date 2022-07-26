package com.github.bobi.osgiannotationprocessor.jps

import org.jetbrains.jps.builders.BuildTargetType
import org.jetbrains.jps.incremental.BuilderService
import org.jetbrains.jps.incremental.TargetBuilder

class OSGIScrBuilderService : BuilderService() {

    override fun getTargetTypes(): List<BuildTargetType<*>> {
        return listOf(OSGIScrBuildTargetType.INSTANCE)
    }

    override fun createBuilders(): List<TargetBuilder<*, *>> {
        return listOf(OSGIScrBuilder())
    }
}