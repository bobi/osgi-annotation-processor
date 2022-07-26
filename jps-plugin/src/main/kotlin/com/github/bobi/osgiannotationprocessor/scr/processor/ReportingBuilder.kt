package com.github.bobi.osgiannotationprocessor.scr.processor

import aQute.bnd.osgi.Analyzer
import aQute.bnd.osgi.Builder
import aQute.bnd.osgi.Jar
import aQute.service.reporter.Report
import aQute.service.reporter.Reporter
import com.github.bobi.osgiannotationprocessor.jps.OSGIScrBuildTarget
import com.github.bobi.osgiannotationprocessor.scr.logger.ScrLogger
import com.github.bobi.osgiannotationprocessor.scr.logger.SourceLocation
import com.github.bobi.osgiannotationprocessor.settings.OSGIScrConfiguration
import com.github.bobi.osgiannotationprocessor.settings.OSGIScrSpec
import org.apache.felix.scrplugin.bnd.SCRDescriptorBndPlugin
import java.io.File
import java.io.IOException
import java.util.*

class ReportingBuilder(
    private val target: OSGIScrBuildTarget,
    private val classDir: File,
    private val logger: ScrLogger
) : Builder() {

    private val configuration: OSGIScrConfiguration = target.configuration

    private val moduleName: String = target.module.name

    init {
        this.isTrace = logger.isDebugEnabled
        this.base = classDir
        this.setJar(classDir)
    }

    override fun error(string: String, vararg args: Any): Reporter.SetLocation {
        val errLocation = super.error(string, *args)

        val location = errLocation.location()

        logger.error(location.message, toSourceLocation(location))

        return errLocation
    }

    override fun warning(string: String, vararg args: Any): Reporter.SetLocation {
        val warnLocation = super.warning(string, *args)

        val location = warnLocation.location()

        logger.warn(location.message, toSourceLocation(location))

        return warnLocation
    }

    override fun build(): Jar {
        configure()

        return super.build()
    }

    private fun configure() {
        this.properties = buildProperties()
        this.setClasspath(buildClasspath())
    }

    private fun buildProperties(): Properties {
        val properties = Properties()

        properties[Analyzer.BUNDLE_SYMBOLICNAME] = moduleName
        properties[Analyzer.DSANNOTATIONS] = "*"
        properties[Analyzer.METATYPE_ANNOTATIONS] = "*"
        properties[Analyzer.IMPORT_PACKAGE] = "*"

        if (configuration.manualSpec) {
            properties[Analyzer.DSANNOTATIONS_OPTIONS] =
                "version;minimum=${configuration.spec.version};maximum=${configuration.spec.version}"
        }

        properties[Analyzer.STRICT] = configuration.strictMode.toString()

        addFelixPluginProperties(properties)

        return properties
    }

    private fun addFelixPluginProperties(properties: Properties) {
        //TODO: for future. Instead of using static felix scr dependency, lookup scr plugin in classpath and add it only when found
        if (configuration.felixEnabled) {
            val felixScrPluginOptionsMap = mutableMapOf<String, String>(
                "strictMode" to configuration.strictMode.toString(),
                "generateAccessors" to configuration.generateAccessors.toString(),
                "log" to if (configuration.debugLogging) "Debug" else "Warn",
                "destdir" to classDir.canonicalPath
            )

            if (configuration.manualSpec) {
                felixScrPluginOptionsMap["specVersion"] = OSGIScrSpec.felixSpec(configuration.spec).version
            }

            val felixScrPluginOptions = felixScrPluginOptionsMap
                .map { "${it.key}=${it.value}" }
                .joinToString(separator = ";")

            properties[PLUGIN] =
                "${SCRDescriptorBndPlugin::class.java.name};${felixScrPluginOptions}"
                    .replace("[\r\n]".toRegex(), "")
        }
    }

    @Throws(IOException::class)
    private fun buildClasspath(): List<Jar> {
        val classpath = mutableListOf<Jar>()

        if (classDir.isDirectory) {
            classpath.add(Jar(moduleName, classDir))
        }

        val moduleClasspath = target.getModuleClasspath()

        logger.debug(
            "classpath: ${
                moduleClasspath.joinToString(
                    prefix = "[",
                    postfix = "]",
                    separator = ", "
                ) { it.canonicalPath }
            }"
        )

        for (cpe in moduleClasspath) {
            if (cpe.exists()) {
                classpath.add(Jar(cpe))
            } else {
                logger.warn("Path ${cpe.canonicalPath} does not exist")
            }
        }

        return classpath
    }

    private fun toSourceLocation(location: Report.Location): SourceLocation {
        if (location.file?.endsWith(".class") == true) {
            val moduleSourceRoots = target.getModuleSourceRoots()
            val moduleOutputRoots = target.getModuleOutputRoots()
            val out = moduleOutputRoots.find { location.file.contains(it.canonicalPath) }

            if (out != null) {
                val loc = File(location.file).canonicalPath

                val relativePath = loc.substring(out.canonicalPath.length, loc.length - 6) + ".java"
                for (root in moduleSourceRoots) {
                    val file = File(root, relativePath)
                    if (file.exists()) {
                        return SourceLocation(file.canonicalPath, location.line)
                    }
                }
            }
        }

        return SourceLocation(location.file, location.line)
    }
}
