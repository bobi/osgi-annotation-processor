// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jps.builders

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.ex.PathManagerEx
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.TimeoutUtil
import com.intellij.util.io.DirectoryContentSpec
import com.intellij.util.io.TestFileSystemBuilder
import com.intellij.util.io.assertMatches
import org.jetbrains.jps.api.CanceledStatus
import org.jetbrains.jps.builders.impl.BuildDataPathsImpl
import org.jetbrains.jps.builders.impl.BuildRootIndexImpl
import org.jetbrains.jps.builders.impl.BuildTargetIndexImpl
import org.jetbrains.jps.builders.impl.BuildTargetRegistryImpl
import org.jetbrains.jps.builders.logging.BuildLoggingManager
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.cmdline.ClasspathBootstrap
import org.jetbrains.jps.cmdline.ProjectDescriptor
import org.jetbrains.jps.incremental.BuilderRegistry
import org.jetbrains.jps.incremental.FSOperations
import org.jetbrains.jps.incremental.IncProjectBuilder
import org.jetbrains.jps.incremental.RebuildRequestedException
import org.jetbrains.jps.incremental.fs.BuildFSState
import org.jetbrains.jps.incremental.relativizer.PathRelativizerService
import org.jetbrains.jps.incremental.storage.BuildDataManager
import org.jetbrains.jps.incremental.storage.BuildTargetsState
import org.jetbrains.jps.incremental.storage.ProjectStamps
import org.jetbrains.jps.indices.ModuleExcludeIndex
import org.jetbrains.jps.indices.impl.IgnoredFileIndexImpl
import org.jetbrains.jps.indices.impl.ModuleExcludeIndexImpl
import org.jetbrains.jps.model.*
import org.jetbrains.jps.model.java.*
import org.jetbrains.jps.model.java.compiler.JavaCompilers
import org.jetbrains.jps.model.java.impl.JavaModuleIndexImpl
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.library.sdk.JpsSdk
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.model.serialization.JpsProjectLoader
import org.jetbrains.jps.model.serialization.PathMacroUtil
import org.jetbrains.jps.util.JpsPathUtil
import java.io.File
import java.io.IOException
import java.nio.file.Paths

abstract class JpsBuildTestCase : UsefulTestCase() {
    private var myProjectDir: File? = null

    private val myJdk: JpsSdk<JpsDummyElement> by lazy { addJdk("jdk") }

    private lateinit var myProject: JpsProject
    private lateinit var myModel: JpsModel
    private lateinit var myDataStorageRoot: File
    private lateinit var myLogger: TestProjectBuilderLogger
    private lateinit var myBuildParams: Map<String, String>

    protected val projectName: String
        get() = StringUtil.decapitalize(StringUtil.trimStart(name, "test"))

    protected val jdk: JpsSdk<JpsDummyElement> get() = myJdk

    override fun setUp() {
        super.setUp()
        myModel = JpsElementFactory.getInstance().createModel()
        myProject = myModel.project
        myDataStorageRoot = FileUtil.createTempDirectory("compile-server-$projectName", null)
        myLogger = TestProjectBuilderLogger()
        myBuildParams = HashMap()
    }

    protected fun addJdk(name: String): JpsSdk<JpsDummyElement> {
        return try {
            val pathToRtJar = ClasspathBootstrap.getResourcePath(Any::class.java)
            val path =
                if (pathToRtJar == null) null else FileUtil.toSystemIndependentName(File(pathToRtJar).canonicalPath)
            addJdk(name, path)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    protected fun addJdk(name: String, jdkClassesRoot: String?): JpsSdk<JpsDummyElement> {
        val homePath = System.getProperty("java.home")
        val versionString = System.getProperty("java.version")
        val jdk = myModel.global
            .addSdk(name, homePath, versionString, JpsJavaSdkType.INSTANCE)
        if (jdkClassesRoot != null) {
            jdk.addRoot(JpsPathUtil.pathToUrl(jdkClassesRoot), JpsOrderRootType.COMPILED)
        }
        return jdk.properties
    }

    protected fun createProjectDescriptor(buildLoggingManager: BuildLoggingManager?): ProjectDescriptor {
        return try {
            val targetRegistry = BuildTargetRegistryImpl(myModel)
            val index: ModuleExcludeIndex = ModuleExcludeIndexImpl(myModel)
            val ignoredFileIndex = IgnoredFileIndexImpl(myModel)
            val dataPaths: BuildDataPaths = BuildDataPathsImpl(myDataStorageRoot)
            val buildRootIndex = BuildRootIndexImpl(
                targetRegistry,
                myModel,
                index,
                dataPaths,
                ignoredFileIndex
            )
            val targetIndex = BuildTargetIndexImpl(targetRegistry, buildRootIndex)
            val targetsState = BuildTargetsState(dataPaths, myModel, buildRootIndex)
            val relativizer = PathRelativizerService(myModel.project)
            val projectStamps = ProjectStamps(myDataStorageRoot, targetsState, relativizer)
            val dataManager = BuildDataManager(dataPaths, targetsState, relativizer)
            ProjectDescriptor(
                myModel,
                BuildFSState(true),
                projectStamps,
                dataManager,
                buildLoggingManager,
                index,
                targetIndex,
                buildRootIndex,
                ignoredFileIndex
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    protected fun loadProject(
        projectPath: String,
        pathVariables: Map<String?, String> = emptyMap()
    ) {
        try {
            val testDataRootPath = testDataRootPath
            val fullProjectPath =
                FileUtil.toSystemDependentName(if (testDataRootPath != null) "$testDataRootPath/$projectPath" else projectPath)
            val allPathVariables: MutableMap<String?, String> = HashMap(pathVariables.size + 1)
            allPathVariables.putAll(pathVariables)
            allPathVariables[PathMacroUtil.APPLICATION_HOME_DIR] = PathManager.getHomePath()
            allPathVariables.putAll(additionalPathVariables)
            JpsProjectLoader.loadProject(myProject, allPathVariables, Paths.get(fullProjectPath))
            val config = JpsJavaExtensionService.getInstance()
                .getCompilerConfiguration(myProject)
            config.getCompilerOptions(JavaCompilers.JAVAC_ID).PREFER_TARGET_JDK_COMPILER = false
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    protected val additionalPathVariables: Map<String?, String>
        get() = emptyMap()
    protected val testDataRootPath: String?
        get() = null

    protected fun <T : JpsElement?> addModule(
        moduleName: String,
        srcPaths: Array<String>,
        outputPath: String?,
        testOutputPath: String?,
        sdk: JpsSdk<T>
    ): JpsModule {
        val module = myProject.addModule(moduleName, JpsJavaModuleType.INSTANCE)
        setupModuleSdk(module, sdk)
        if (srcPaths.isNotEmpty() || outputPath != null) {
            for (srcPath in srcPaths) {
                module.contentRootsList.addUrl(JpsPathUtil.pathToUrl(srcPath))
                module.addSourceRoot(JpsPathUtil.pathToUrl(srcPath), JavaSourceRootType.SOURCE)
            }
            val extension = JpsJavaExtensionService.getInstance().getOrCreateModuleExtension(module)
            if (outputPath != null) {
                extension.outputUrl = JpsPathUtil.pathToUrl(outputPath)
                if (!StringUtil.isEmpty(testOutputPath)) {
                    extension.testOutputUrl = JpsPathUtil.pathToUrl(testOutputPath)
                } else {
                    extension.testOutputUrl = extension.outputUrl
                }
            } else {
                extension.isInheritOutput = true
            }
        }
        return module
    }

    protected fun <T : JpsElement?> setupModuleSdk(module: JpsModule, sdk: JpsSdk<T>) {
        val sdkType = sdk.sdkType
        val sdkTable = module.sdkReferencesTable
        sdkTable.setSdkReference(sdkType, sdk.createReference())
        if (sdkType is JpsJavaSdkTypeWrapper) {
            val wrapperRef = sdk.createReference()
            sdkTable.setSdkReference(
                JpsJavaSdkType.INSTANCE, JpsJavaExtensionService.getInstance().createWrappedJavaSdkReference(
                    (sdkType as JpsJavaSdkTypeWrapper), wrapperRef
                )
            )
        }
        // ensure jdk entry is the first one in dependency list
        module.dependenciesList.clear()
        module.dependenciesList.addSdkDependency(sdkType)
        module.dependenciesList.addModuleSourceDependency()
    }

    protected fun rebuildAllModules() {
        doBuild(CompileScopeTestBuilder.rebuild().allModules()).assertSuccessful()
    }

    protected fun buildAllModules(): BuildResult {
        return doBuild(CompileScopeTestBuilder.make().allModules())
    }

    protected fun doBuild(scope: CompileScopeTestBuilder): BuildResult {
        val descriptor = createProjectDescriptor(BuildLoggingManager(myLogger))
        return try {
            myLogger.clearFilesData()
            doBuild(descriptor, scope)
        } finally {
            descriptor.release()
        }
    }

    protected fun clearBuildLog() {
        myLogger.clearLog()
    }

    fun assertCompiled(builderName: String, vararg paths: String) {
        myLogger.assertCompiled(builderName, arrayOf(myProjectDir, myDataStorageRoot), *paths)
    }

    fun checkFullLog(expectedLogFile: File) {
        assertSameLinesWithFile(
            expectedLogFile.absolutePath,
            myLogger.getFullLog(myProjectDir, myDataStorageRoot)
        )
    }

    protected fun assertDeleted(vararg paths: String) {
        myLogger.assertDeleted(arrayOf(myProjectDir, myDataStorageRoot), *paths)
    }

    protected fun doBuild(descriptor: ProjectDescriptor, scopeBuilder: CompileScopeTestBuilder): BuildResult {
        val builder = IncProjectBuilder(
            descriptor,
            BuilderRegistry.getInstance(),
            myBuildParams,
            CanceledStatus.NULL,
            true
        )
        val result = BuildResult()
        builder.addMessageHandler(result)
        try {
            beforeBuildStarted(descriptor)
            builder.build(scopeBuilder.build(), false)
            result.storeMappingsDump(descriptor)
        } catch (e: RebuildRequestedException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } finally {
            // the following code models module index reload after each make session
            val moduleIndex = JpsJavaExtensionService.getInstance()
                .getJavaModuleIndex(descriptor.project)
            if (moduleIndex is JavaModuleIndexImpl) {
                moduleIndex.dropCache()
            }
        }
        return result
    }

    protected fun beforeBuildStarted(descriptor: ProjectDescriptor) {}

    protected fun deleteFile(relativePath: String) {
        delete(File(orCreateProjectDir, relativePath).absolutePath)
    }

    protected fun changeFile(relativePath: String, newContent: String? = null) {
        change(File(orCreateProjectDir, relativePath).absolutePath, newContent)
    }

    protected fun createFile(relativePath: String): String {
        return createFile(relativePath, "")
    }

    protected fun createDir(relativePath: String): String {
        val dir = File(orCreateProjectDir, relativePath)
        val created = dir.mkdirs()
        if (!created && !dir.isDirectory) {
            fail("Cannot create " + dir.absolutePath + " directory")
        }
        return FileUtil.toSystemIndependentName(dir.absolutePath)
    }

    fun createFile(relativePath: String, text: String): String {
        return try {
            val file = File(orCreateProjectDir, relativePath)
            FileUtil.writeToFile(file, text)
            FileUtil.toSystemIndependentName(file.absolutePath)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    protected fun copyToProject(relativeSourcePath: String, relativeTargetPath: String): String {
        val source = findFindUnderProjectHome(relativeSourcePath)
        val fullTargetPath = getAbsolutePath(relativeTargetPath)
        val target = File(fullTargetPath)
        try {
            FileUtil.copyFileOrDir(source, target)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return fullTargetPath
    }

    protected fun findFindUnderProjectHome(relativeSourcePath: String): File {
        return PathManagerEx.findFileUnderProjectHome(relativeSourcePath, javaClass)
    }

    val orCreateProjectDir: File?
        get() {
            if (myProjectDir == null) {
                myProjectDir = try {
                    doGetProjectDir()
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
            return myProjectDir
        }

    protected fun doGetProjectDir(): File {
        return FileUtil.createTempDirectory("prj", null)
    }

    fun getAbsolutePath(pathRelativeToProjectRoot: String): String {
        return FileUtil.toSystemIndependentName(File(orCreateProjectDir, pathRelativeToProjectRoot).absolutePath)
    }

    fun addModule(moduleName: String, vararg srcPaths: String): JpsModule {
        return addModule(
            moduleName,
            arrayOf(*srcPaths),
            getAbsolutePath(getModuleOutputRelativePath(moduleName)),
            null,
            jdk
        )
    }

    protected fun getModuleOutputRelativePath(module: JpsModule): String {
        return getModuleOutputRelativePath(module.name)
    }

    protected fun getModuleOutputRelativePath(moduleName: String): String {
        return "out/production/$moduleName"
    }

    protected fun checkMappingsAreSameAfterRebuild(makeResult: BuildResult) {
        val makeDump = makeResult.mappingsDump
        val rebuildResult = doBuild(CompileScopeTestBuilder.rebuild().allModules())
        rebuildResult.assertSuccessful()
        val rebuildDump = rebuildResult.mappingsDump
        assertEquals(rebuildDump, makeDump)
    }

    companion object {
        protected fun rename(path: String?, newName: String) {
            try {
                val file = File(FileUtil.toSystemDependentName(path!!))
                assertTrue("File " + file.absolutePath + " doesn't exist", file.exists())
                val tempFile = File(file.parentFile, "__$newName")
                FileUtil.rename(file, tempFile)
                val newFile = File(file.parentFile, newName)
                FileUtil.copyContent(tempFile, newFile)
                FileUtil.delete(tempFile)
                change(newFile.path)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        protected fun assertOutput(outputPath: String?, expected: TestFileSystemBuilder) {
            expected.build().assertDirectoryEqual(
                File(
                    FileUtil.toSystemDependentName(
                        outputPath!!
                    )
                )
            )
        }

        protected fun assertOutput(outputPath: String, expected: DirectoryContentSpec) {
            File(outputPath).assertMatches(expected)
        }

        protected fun assertOutput(module: JpsModule, expected: TestFileSystemBuilder) {
            val outputUrl = JpsJavaExtensionService.getInstance().getOutputUrl(module, false)
            assertNotNull(outputUrl)
            assertOutput(JpsPathUtil.urlToPath(outputUrl), expected)
        }

        protected fun assertOutput(module: JpsModule, expected: DirectoryContentSpec) {
            val outputUrl = JpsJavaExtensionService.getInstance().getOutputUrl(module, false)
            assertNotNull(outputUrl)
            assertOutput(JpsPathUtil.urlToPath(outputUrl), expected)
        }

        protected fun change(filePath: String?, newContent: String? = null) {
            try {
                val file = File(FileUtil.toSystemDependentName(filePath!!))
                assertTrue("File " + file.absolutePath + " doesn't exist", file.exists())
                if (newContent != null) {
                    FileUtil.writeToFile(file, newContent)
                }
                val oldTimestamp = FSOperations.lastModified(file)
                val time = System.currentTimeMillis()
                setLastModified(file, time)
                if (FSOperations.lastModified(file) <= oldTimestamp) {
                    setLastModified(file, time + 1)
                    var newTimeStamp = FSOperations.lastModified(file)
                    if (newTimeStamp <= oldTimestamp) {
                        //Mac OS and some versions of Linux truncates timestamp to nearest second
                        setLastModified(file, time + 1000)
                        newTimeStamp = FSOperations.lastModified(file)
                        assertTrue("Failed to change timestamp for " + file.absolutePath, newTimeStamp > oldTimestamp)
                    }
                    sleepUntil(newTimeStamp)
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        protected fun sleepUntil(time: Long) {
            //we need this to ensure that the file won't be treated as changed by user during compilation and therefore marked for recompilation
            var delta: Long
            while (time - System.currentTimeMillis().also { delta = it } > 0) {
                TimeoutUtil.sleep(delta)
            }
        }

        private fun setLastModified(file: File, time: Long) {
            val updated = file.setLastModified(time)
            assertTrue("Cannot modify timestamp for " + file.absolutePath, updated)
        }

        protected fun delete(filePath: String?) {
            val file = File(FileUtil.toSystemDependentName(filePath!!))
            assertTrue("File " + file.absolutePath + " doesn't exist", file.exists())
            val deleted = FileUtil.delete(file)
            assertTrue("Cannot delete file " + file.absolutePath, deleted)
        }

        protected fun getModuleOutput(module: JpsModule?): File {
            val outputUrl = JpsJavaExtensionService.getInstance().getOutputUrl(module, false)
            return JpsPathUtil.urlToFile(outputUrl)
        }
    }
}
