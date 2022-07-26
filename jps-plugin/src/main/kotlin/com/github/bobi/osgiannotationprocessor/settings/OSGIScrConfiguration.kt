package com.github.bobi.osgiannotationprocessor.settings

import com.github.bobi.osgiannotationprocessor.jps.OSGIScrBuilder
import org.jetbrains.jps.model.JpsElementChildRole
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase

interface OSGIScrConfiguration {
    companion object {
        val ROLE: JpsElementChildRole<OSGIScrConfigurationImpl> =
            JpsElementChildRoleBase.create(OSGIScrBuilder.ID)
    }

    var enabled: Boolean

    var manualSpec: Boolean

    var spec: OSGIScrSpec

    var skipTests: Boolean

    var debugLogging: Boolean

    var felixEnabled: Boolean


    var generateAccessors: Boolean

    var strictMode: Boolean

    var optimizedBuild: Boolean
}