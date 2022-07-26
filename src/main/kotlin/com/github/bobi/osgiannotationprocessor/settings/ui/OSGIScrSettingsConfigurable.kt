package com.github.bobi.osgiannotationprocessor.settings.ui

import com.github.bobi.osgiannotationprocessor.settings.OSGIScrProjectSettings
import com.github.bobi.osgiannotationprocessor.settings.OSGIScrSpec
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.*

/**
 * User: Andrey Bardashevsky
 * Date/Time: 26.07.2022 01:34
 */
class OSGIScrSettingsConfigurable(private val project: Project) : BoundConfigurable("OSGI SCR Annotation Processor") {

    private val specVersionsModel =
        CollectionComboBoxModel(OSGIScrSpec.values().map { SpecComboDecorator(it) }, SpecComboDecorator(settings.spec))

    private val settings get() = OSGIScrProjectSettings.getInstance(project)

    override fun createPanel(): DialogPanel = panel {
        lateinit var enableCheckBox: CellBuilder<JBCheckBox>

        row {
            enableCheckBox = checkBox("Enable")
                .withSelectedBinding(settings::enabled.toBinding())
        }

        titledRow("General Settings") {
            lateinit var manualSpecCheckBox: CellBuilder<JBCheckBox>

            row {
                manualSpecCheckBox = checkBox("Set Specification Version Manually")
                    .withSelectedBinding(settings::manualSpec.toBinding())
                    .enableIf(enableCheckBox.selected)
            }
            row("Specification Version:") {
                comboBox(
                    specVersionsModel,
                    { SpecComboDecorator(settings.spec) },
                    { if (it?.spec != null) settings.spec = it.spec })
                    .withLeftGap()
                    .enableIf(enableCheckBox.selected and manualSpecCheckBox.selected)
            }
            row {
                checkBox("Strict Mode")
                    .withSelectedBinding(settings::strictMode.toBinding())
                    .enableIf(enableCheckBox.selected)
            }
            row {
                checkBox("Skip Tests")
                    .withSelectedBinding(settings::skipTests.toBinding())
                    .enableIf(enableCheckBox.selected)
            }
            row {
                checkBox("Debug Logging")
                    .withSelectedBinding(settings::debugLogging.toBinding())
                    .enableIf(enableCheckBox.selected)
            }
        }

        blockRow {
            lateinit var felixEnableCheckBox: CellBuilder<JBCheckBox>
            row {
                felixEnableCheckBox = checkBox("Enable Felix SCR Annotation Processing")
                    .withSelectedBinding(settings::felixEnabled.toBinding())
                    .enableIf(enableCheckBox.selected)
            }

            titledRow("Felix Specific Settings") {
                row {
                    checkBox("Generate Accessors")
                        .withSelectedBinding(settings::generateAccessors.toBinding())
                        .enableIf(enableCheckBox.selected and felixEnableCheckBox.selected)
                }
                row {
                    checkBox("Optimized Build")
                        .withSelectedBinding(settings::optimizedBuild.toBinding())
                        .enableIf(enableCheckBox.selected and felixEnableCheckBox.selected)
                }
            }
        }
    }

    private class SpecComboDecorator(val spec: OSGIScrSpec) {
        override fun toString(): String {
            return spec.version
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SpecComboDecorator) return false

            if (spec != other.spec) return false

            return true
        }

        override fun hashCode(): Int {
            return spec.hashCode()
        }
    }
}