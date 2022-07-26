package com.github.bobi.osgiannotationprocessor.settings

enum class OSGIScrSpec(val version: String) {
    SPEC_1_0("1.0"),
    SPEC_1_1("1.1"),
    SPEC_1_2("1.2"),
    SPEC_1_3("1.3"),
    SPEC_1_4("1.4"),
    SPEC_2_0("2.0");

    companion object {
        fun versions() = OSGIScrSpec.values().map { it.version }

        fun felixSpec(spec: OSGIScrSpec): OSGIScrSpec {
            if (spec > SPEC_1_2) {
                return SPEC_1_2
            }

            return spec
        }
    }
}