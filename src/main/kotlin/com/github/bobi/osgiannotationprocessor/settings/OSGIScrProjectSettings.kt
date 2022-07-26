package com.github.bobi.osgiannotationprocessor.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.OptionTag

/**
 * Storage for AEM project settings.
 *
 * @author Kostiantyn Diachenko
 */
@State(
    name = "OsgiScrSettings",
    storages = [(Storage("osgi-scr.xml"))]
)
class OSGIScrProjectSettings : PersistentStateComponent<OSGIScrProjectSettings>, OSGIScrConfiguration {

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

    override fun getState(): OSGIScrProjectSettings = this

    override fun loadState(state: OSGIScrProjectSettings) {
        enabled = state.enabled
        manualSpec = state.manualSpec
        spec = state.spec
        strictMode = state.strictMode
        skipTests = state.skipTests
        debugLogging = state.debugLogging

        felixEnabled = state.felixEnabled
        generateAccessors = state.generateAccessors
        optimizedBuild = state.optimizedBuild
    }

    companion object {

        fun getInstance(project: Project): OSGIScrProjectSettings =
            project.getService(OSGIScrProjectSettings::class.java)

    }
}
