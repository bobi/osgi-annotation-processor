package com.github.bobi.osgiannotationprocessor.jps

import com.github.bobi.osgiannotationprocessor.settings.OSGIScrConfiguration
import org.jetbrains.jps.builders.BuildTargetLoader
import org.jetbrains.jps.builders.ModuleBasedBuildTargetType
import org.jetbrains.jps.model.JpsModel

class OSGIScrBuildTargetType private constructor() : ModuleBasedBuildTargetType<OSGIScrBuildTarget>(OSGIScrBuilder.ID) {
    override fun computeAllTargets(model: JpsModel): List<OSGIScrBuildTarget> {
        val targets = mutableListOf<OSGIScrBuildTarget>()
        val configuration: OSGIScrConfiguration = JPSSCRExtensionService.getConfiguration(model.project)

        if (configuration.enabled) {
            for (module in model.project.modules) {
                targets.add(OSGIScrBuildTarget(configuration, module))
            }
        }

        return targets
    }

    override fun createLoader(model: JpsModel): BuildTargetLoader<OSGIScrBuildTarget> {
        return Loader(model)
    }

    private class Loader(model: JpsModel) : BuildTargetLoader<OSGIScrBuildTarget>() {
        private val targets: MutableMap<String, OSGIScrBuildTarget> = HashMap()

        init {
            val configuration: OSGIScrConfiguration = JPSSCRExtensionService.getConfiguration(model.project)

            if (configuration.enabled) {
                for (module in model.project.modules) {
                    targets[module.name] = OSGIScrBuildTarget(configuration, module)
                }
            }
        }

        override fun createTarget(targetId: String): OSGIScrBuildTarget? {
            return targets[targetId]
        }
    }

    companion object {
        val INSTANCE = OSGIScrBuildTargetType()
    }
}