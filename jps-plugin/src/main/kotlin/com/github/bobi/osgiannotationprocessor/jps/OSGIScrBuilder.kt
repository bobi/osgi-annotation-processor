package com.github.bobi.osgiannotationprocessor.jps

import com.github.bobi.osgiannotationprocessor.scr.logger.ScrLoggerImpl
import com.github.bobi.osgiannotationprocessor.scr.processor.ScrProcessor
import org.jetbrains.jps.builders.BuildOutputConsumer
import org.jetbrains.jps.builders.BuildRootDescriptor
import org.jetbrains.jps.builders.DirtyFilesHolder
import org.jetbrains.jps.builders.logging.ProjectBuilderLogger
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.TargetBuilder
import org.jetbrains.jps.incremental.messages.ProgressMessage

class OSGIScrBuilder :
    TargetBuilder<BuildRootDescriptor, OSGIScrBuildTarget>(mutableListOf(OSGIScrBuildTargetType.INSTANCE)) {

    companion object {
        const val ID: String = "osgi-scr"
    }

    override fun getPresentableName(): String = ID

    override fun build(
        target: OSGIScrBuildTarget,
        holder: DirtyFilesHolder<BuildRootDescriptor, OSGIScrBuildTarget>,
        outputConsumer: BuildOutputConsumer,
        compileContext: CompileContext
    ) {
        if (target.configuration.enabled) {
            return doBuild(compileContext, target, holder)
        }

        return
    }

    private fun doBuild(
        compileContext: CompileContext,
        target: OSGIScrBuildTarget,
        holder: DirtyFilesHolder<BuildRootDescriptor, OSGIScrBuildTarget>,
    ) {
        val logger = ScrLoggerImpl(compileContext, target.module.name, presentableName, target.configuration.debugLogging)

        logger.debug("Settings enabled: ${target.configuration.enabled}")
        logger.debug("Settings Felix SCR enabled: ${target.configuration.felixEnabled}")
        logger.debug("Has Dirty files: ${holder.hasDirtyFiles()}")
        logger.debug("Has removed files: ${holder.hasRemovedFiles()}")

        if (target.isTests && target.configuration.skipTests) {
            logger.debug("Skip test compile")
            return
        }

        if (holder.hasDirtyFiles() || holder.hasRemovedFiles()) {
            compileContext.processMessage(ProgressMessage("$presentableName [${target.module.name}]"))

            val p = ScrProcessor(target, logger)

            val result = p.execute()

            val builderLogger: ProjectBuilderLogger = compileContext.loggingManager.projectBuilderLogger

            if (builderLogger.isEnabled) {
                builderLogger.logCompiledFiles(result.generatedFiles, ID, "Generated OSGI SCR:")
            }

            return
        }

        logger.debug("Nothing to do.")
    }
}