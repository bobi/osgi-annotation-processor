// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.builders

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.containers.FileCollectionFactory
import com.intellij.util.containers.MultiMap
import org.jetbrains.jps.builders.impl.logging.ProjectBuilderLoggerBase
import java.io.File

class TestProjectBuilderLogger : ProjectBuilderLoggerBase() {
    private val myCompiledFiles = MultiMap<String, File>()
    private val myDeletedFiles = FileCollectionFactory.createCanonicalFileSet()
    private val myLogLines: MutableList<String> = mutableListOf()

    override fun logDeletedFiles(paths: Collection<String>) {
        super.logDeletedFiles(paths)
        for (path in paths) {
            myDeletedFiles.add(File(path))
        }
    }

    override fun logCompiledFiles(files: Collection<File>, builderId: String, description: String) {
        super.logCompiledFiles(files, builderId, description)
        myCompiledFiles.putValues(builderId, files)
    }

    fun clearFilesData() {
        myCompiledFiles.clear()
        myDeletedFiles.clear()
    }

    fun clearLog() {
        myLogLines.clear()
    }

    fun assertCompiled(builderName: String, baseDirs: Array<File?>, vararg paths: String) {
        assertRelativePaths(baseDirs, myCompiledFiles[builderName], arrayOf(*paths))
    }

    fun assertDeleted(baseDirs: Array<File?>, vararg paths: String) {
        assertRelativePaths(baseDirs, myDeletedFiles, arrayOf(*paths))
    }

    override fun logLine(message: String) {
        myLogLines.add(message)
    }

    fun getFullLog(vararg baseDirs: File?): String {
        return StringUtil.join(myLogLines, { s: String ->
            for (dir in baseDirs) {
                if (dir != null) {
                    val path = FileUtil.toSystemIndependentName(dir.absolutePath) + "/"
                    if (s.startsWith(path)) {
                        return@join s.substring(path.length)
                    }
                }
            }
            s
        }, "\n")
    }

    override fun isEnabled(): Boolean {
        return true
    }

    companion object {
        private fun assertRelativePaths(baseDirs: Array<File?>, files: Collection<File>, expected: Array<String>) {
            val relativePaths: MutableList<String> = ArrayList()
            for (file in files) {
                var path = file.absolutePath
                for (baseDir in baseDirs) {
                    if (baseDir != null && FileUtil.isAncestor(baseDir, file, false)) {
                        path = FileUtil.getRelativePath(baseDir, file)!!
                        break
                    }
                }
                relativePaths.add(FileUtil.toSystemIndependentName(path))
            }
            UsefulTestCase.assertSameElements(relativePaths, *expected)
        }
    }
}