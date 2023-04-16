package com.github.bobi.osgiannotationprocessor.compiler

import com.github.bobi.osgiannotationprocessor.settings.OSGIScrProjectSettings
import com.intellij.compiler.server.BuildProcessParametersProvider
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil

/**
 * User: Andrey Bardashevsky
 * Date/Time: 15.04.2023 23:53
 */
class OSGIBuildProcessParametersProvider(private val project: Project) : BuildProcessParametersProvider() {
    override fun getClassPath(): Iterable<String> {
        val settings = OSGIScrProjectSettings.getInstance(project)

        return if (settings.enabled) jpsClasspath else emptyList()
    }

    companion object {
        private val jpsClasspath = buildClassPath()

        private fun buildClassPath(): List<String> {
            val pluginJarFile = PathManager.getJarForClass(OSGIBuildProcessParametersProvider::class.java)

            val libRoot = pluginJarFile?.parent

            return if (pluginJarFile != null && libRoot != null) {
                val virtualPluginJarFile = VfsUtil.findFileByIoFile(pluginJarFile.toFile(), true)
                val virtualLibRoot = VfsUtil.findFileByIoFile(libRoot.toFile(), true)

                virtualLibRoot?.children
                    ?.filter { vf -> vf != virtualPluginJarFile }
                    ?.mapNotNull { vf -> vf.path }
                    ?: emptyList()
            } else {
                emptyList()
            }
        }
    }
}
