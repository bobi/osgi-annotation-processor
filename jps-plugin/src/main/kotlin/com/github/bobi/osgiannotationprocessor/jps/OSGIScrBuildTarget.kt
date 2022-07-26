package com.github.bobi.osgiannotationprocessor.jps

import com.github.bobi.osgiannotationprocessor.settings.OSGIScrConfiguration
import com.github.bobi.osgiannotationprocessor.settings.OSGIScrConfigurationImpl
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.xmlb.XmlSerializer
import org.jetbrains.jps.builders.*
import org.jetbrains.jps.builders.impl.BuildRootDescriptorImpl
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.cmdline.ProjectDescriptor
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.indices.IgnoredFileIndex
import org.jetbrains.jps.indices.ModuleExcludeIndex
import org.jetbrains.jps.model.JpsModel
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModule
import java.io.File
import java.io.PrintWriter

class OSGIScrBuildTarget(val configuration: OSGIScrConfiguration, module: JpsModule) :
    ModuleBasedTarget<BuildRootDescriptor?>(OSGIScrBuildTargetType.INSTANCE, module) {

    private var sourceRoots: List<File>? = null

    private var outputRoots: List<File>? = null

    private var classpath: List<File>? = null

    override fun getPresentableName(): String = "OSGI SCR in module '${module.name}'"

    override fun isTests(): Boolean = false

    override fun getId(): String = myModule.name

    override fun writeConfiguration(pd: ProjectDescriptor, out: PrintWriter) {
        var configHash = 0

        if (configuration.enabled) {
            val p: OSGIScrConfiguration = (configuration as OSGIScrConfigurationImpl).properties

            configHash = JDOMUtil.write(XmlSerializer.serialize(p)).hashCode()
        }

        out.write(Integer.toHexString(configHash))
    }

    override fun computeDependencies(
        targetRegistry: BuildTargetRegistry,
        outputIndex: TargetOutputIndex
    ): Collection<BuildTarget<*>> {
        val selector = if (configuration.skipTests) {
            BuildTargetRegistry.ModuleTargetSelector.PRODUCTION
        } else {
            BuildTargetRegistry.ModuleTargetSelector.ALL
        }

        return targetRegistry.getModuleBasedTargets(module, selector)
    }

    override fun computeRootDescriptors(
        model: JpsModel,
        index: ModuleExcludeIndex,
        ignoredFileIndex: IgnoredFileIndex,
        dataPaths: BuildDataPaths
    ): List<BuildRootDescriptor> {
        val rootDescriptors = mutableListOf<BuildRootDescriptor>()

        if (configuration.enabled) {
            val dependencies = if (configuration.skipTests) {
                JpsJavaExtensionService.dependencies(module).productionOnly()
            } else {
                JpsJavaExtensionService.dependencies(module)
            }

            dependencies.recursively()
                .processModules {
                    if (it === module) {
                        val root: File? = JpsJavaExtensionService.getInstance().getOutputDirectory(it, false)
                        if (root != null) {
                            rootDescriptors.add(BuildRootDescriptorImpl(this, root, true))
                        }
                    }
                }
        }

        return rootDescriptors
    }

    override fun findRootDescriptor(rootId: String, rootIndex: BuildRootIndex): BuildRootDescriptor? {
        return ContainerUtil.find(
            rootIndex.getTargetRoots<BuildRootDescriptor>(this, null)
        ) { descriptor: BuildRootDescriptor -> descriptor.rootId == rootId }
    }

    override fun getOutputRoots(context: CompileContext): Collection<File> {
        return getModuleOutputRoots()
    }

    fun getModuleOutputRoots(): Collection<File> {
        if (outputRoots == null) {
            val roots = mutableListOf<File?>()

            val jpsJavaExtensionService = JpsJavaExtensionService.getInstance()

            roots.add(jpsJavaExtensionService.getOutputDirectory(module, false))

            if (!configuration.skipTests) {
                roots.add(jpsJavaExtensionService.getOutputDirectory(module, true))
            }

            outputRoots = roots.filterNotNull()
        }

        return outputRoots ?: emptyList()
    }

    fun getModuleSourceRoots(): List<File> {
        if (sourceRoots == null) {
            val roots = mutableListOf<File?>()

            module.sourceRoots.forEach {
                roots.add(it.file)
            }

            sourceRoots = roots.filterNotNull()
        }

        return sourceRoots ?: emptyList()
    }

    fun getModuleClasspath(): List<File> {
        if (classpath == null) {
            val cp = mutableListOf<File?>()

            val dependencies = if (configuration.skipTests) {
                JpsJavaExtensionService.dependencies(module).productionOnly()
            } else {
                JpsJavaExtensionService.dependencies(module)
            }

            val classes = dependencies.withoutSdk().recursively().classes()

            for (f in classes.roots) {
                // filter out non-Java classpath entries, because Felix fails processing them
                if (f.name.endsWith(".class") || f.name.endsWith(".jar") || f.isDirectory) {
                    cp.add(f)
                }
            }

            classpath = cp.filterNotNull()
        }

        return classpath ?: emptyList()
    }
}