package com.github.bobi.osgiannotationprocessor.settings

import com.intellij.util.xmlb.annotations.OptionTag

/**
 * User: Andrey Bardashevsky
 * Date/Time: 25.07.2022 20:32
 */
class OSGIScrConfigurationProperties: OSGIScrConfiguration {

    @OptionTag
    override var enabled: Boolean = false

    @OptionTag
    override var manualSpec: Boolean = false

    @OptionTag
    override var spec: OSGIScrSpec = OSGIScrSpec.SPEC_1_1

    @OptionTag
    override var strictMode: Boolean = false

    @OptionTag
    override var skipTests: Boolean = true

    @OptionTag
    override var debugLogging: Boolean = false

    @OptionTag
    override var felixEnabled: Boolean = false

    @OptionTag
    override var generateAccessors: Boolean = true

    @OptionTag
    override var optimizedBuild: Boolean = true
}