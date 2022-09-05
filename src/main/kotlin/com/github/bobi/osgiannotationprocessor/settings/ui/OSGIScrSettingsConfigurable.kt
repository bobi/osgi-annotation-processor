package com.github.bobi.osgiannotationprocessor.settings.ui

import com.github.bobi.osgiannotationprocessor.settings.OSGIScrProjectSettings
import com.github.bobi.osgiannotationprocessor.settings.OSGIScrSpec
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.and

/**
 * User: Andrey Bardashevsky
 * Date/Time: 26.07.2022 01:34
 */
class OSGIScrSettingsConfigurable(private val project: Project) : BoundConfigurable("OSGI SCR Annotation Processor") {

    private val specVersionsModel =
        CollectionComboBoxModel(OSGIScrSpec.values().map { SpecComboDecorator(it) }, SpecComboDecorator(settings.spec))

    private val settings get() = OSGIScrProjectSettings.getInstance(project)

    @Suppress("DialogTitleCapitalization")
    override fun createPanel() = panel {
        lateinit var enableCheckBox: Cell<JBCheckBox>

        row {
            enableCheckBox = checkBox("Enable")
                .bindSelected(settings::enabled)
        }

        group("General Settings") {
            lateinit var manualSpecCheckBox: Cell<JBCheckBox>

            row {
                manualSpecCheckBox = checkBox("Set Specification Version Manually")
                    .bindSelected(settings::manualSpec)
                    .enabledIf(enableCheckBox.selected)
            }
            row("Specification Version:") {
                comboBox(specVersionsModel)
                    .bindItem(
                        { SpecComboDecorator(settings.spec) },
                        { if (it?.spec != null) settings.spec = it.spec }
                    )
                    .enabledIf(enableCheckBox.selected and manualSpecCheckBox.selected)
            }
            row {
                checkBox("Strict Mode")
                    .bindSelected(settings::strictMode)
                    .enabledIf(enableCheckBox.selected)
            }
            row {
                checkBox("Skip Tests")
                    .bindSelected(settings::skipTests)
                    .enabledIf(enableCheckBox.selected)
            }
            row {
                checkBox("Debug Logging")
                    .bindSelected(settings::debugLogging)
                    .enabledIf(enableCheckBox.selected)
            }
        }

        group("Felix Specific Settings") {
            lateinit var felixEnableCheckBox: Cell<JBCheckBox>
            row {
                felixEnableCheckBox = checkBox("Enable Felix SCR Annotation Processing")
                    .bindSelected(settings::felixEnabled)
                    .enabledIf(enableCheckBox.selected)
            }

            row {
                checkBox("Generate Accessors")
                    .bindSelected(settings::generateAccessors)
                    .enabledIf(enableCheckBox.selected and felixEnableCheckBox.selected)
            }
            row {
                checkBox("Optimized Build")
                    .bindSelected(settings::optimizedBuild)
                    .enabledIf(enableCheckBox.selected and felixEnableCheckBox.selected)
            }
        }
    }

    private data class SpecComboDecorator(val spec: OSGIScrSpec) {
        override fun toString() = spec.version
    }
}