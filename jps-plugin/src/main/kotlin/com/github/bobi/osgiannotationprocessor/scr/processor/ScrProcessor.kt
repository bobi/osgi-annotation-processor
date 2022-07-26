package com.github.bobi.osgiannotationprocessor.scr.processor

import aQute.bnd.osgi.Jar
import com.github.bobi.osgiannotationprocessor.commons.Consts.OSGI_INF
import com.github.bobi.osgiannotationprocessor.jps.OSGIScrBuildTarget
import com.github.bobi.osgiannotationprocessor.scr.logger.ScrLogger
import java.io.File
import java.io.FileOutputStream

class ScrProcessor(
    private val target: OSGIScrBuildTarget,
    private val logger: ScrLogger
) {

    private val cleaner: Cleaner = Cleaner(target, logger)

    fun execute(): ExecutionResult {
        val classDirs = target.getModuleOutputRoots()

        if (classDirs.isEmpty()) {
            logger.error("Compiler Output path must be set for: ${target.module.name}")

            return ExecutionResult(false)
        }

        try {
            val generatedFiles = mutableListOf<File>()

            classDirs.forEach {
                logger.debug("Class dir: ${it.path}")

                ReportingBuilder(target, it, logger).use { builder ->
                    cleaner.clean(it)


                    builder.build().use { jar ->
                        generatedFiles.addAll(writeGeneratedResources(jar, it))

                        logger.debug("Built: ${jar.name}")
                    }
                }
            }

            return ExecutionResult(!logger.hasErrors, generatedFiles)
        } catch (e: Exception) {
            logger.error(e)
        }

        return ExecutionResult(false)
    }

    private fun writeGeneratedResources(jar: Jar, classDir: File): Collection<File> {
        val generatedFiles = mutableListOf<File>()

        for ((jarFilePath, value) in jar.resources) {
            if (jarFilePath.startsWith(OSGI_INF)) {
                val outputFile = File(classDir, jarFilePath)
                val dirIndex = jarFilePath.lastIndexOf('/')
                if (dirIndex > 0) {
                    val dirPath = jarFilePath.substring(0, dirIndex)
                    val outDir = File(classDir, dirPath)
                    outDir.mkdirs()
                }
                try {
                    FileOutputStream(outputFile).use { out ->
                        if (logger.isDebugEnabled) {
                            logger.debug("Writing: ${outputFile.canonicalPath}")
                        }
                        value.write(out)

                        generatedFiles.add(outputFile)
                    }
                } catch (e: Exception) {
                    logger.error(e)
                }
            }
        }

        return generatedFiles.toList()
    }
}