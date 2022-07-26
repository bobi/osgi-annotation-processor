package com.github.bobi.osgiannotationprocessor.scr.processor

import com.github.bobi.osgiannotationprocessor.commons.Consts
import com.github.bobi.osgiannotationprocessor.jps.OSGIScrBuildTarget
import com.github.bobi.osgiannotationprocessor.scr.logger.ScrLogger
import java.io.File

/**
 * User: Andrey Bardashevsky
 * Date/Time: 26.07.2022 13:52
 */
class Cleaner(target: OSGIScrBuildTarget, private var logger: ScrLogger) {

    private val moduleSourceRoots: List<File> = target.getModuleSourceRoots()

    private val preserved: Collection<String>

    init {
        preserved = collectPreservedFiles()
    }

    fun clean(dir: File) {
        logger.debug("Preserve files: ${preserved.toTypedArray().contentToString()}")

        val xmlDir = File(dir, Consts.OSGI_INF)

        logger.debug("${Consts.OSGI_INF} exists: ${xmlDir.exists()} Is dir: ${xmlDir.isDirectory}")

        if (xmlDir.exists() && xmlDir.isDirectory) {
            val files = xmlDir.listFiles()?.toList() ?: emptyList()

            logger.debug("${Consts.OSGI_INF} has files: ${files.isNotEmpty()}")

            files.stream()
                .filter { !preserved.contains(it.name) }
                .filter { it.name.endsWith(".xml") }
                .peek { logger.debug("Delete SCR xml: ${it.canonicalPath}") }
                .forEach {
                    if (!it.delete()) {
                        logger.warn("Cannot delete SCR xml: ${it.canonicalPath}")
                    }
                }
        }
    }

    private fun collectPreservedFiles(): Collection<String> {
        val sourceRoots = moduleSourceRoots
        val preserved: MutableSet<String> = mutableSetOf()

        for (sourceRoot in sourceRoots) {
            val file = File(sourceRoot, Consts.OSGI_INF)

            if (file.exists()) {
                val files = file.listFiles()?.toList() ?: emptyList()

                files.stream().map { it.name }.forEach { preserved.add(it) }
            }
        }

        return preserved
    }
}