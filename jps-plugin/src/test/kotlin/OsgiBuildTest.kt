package org.jetbrains.osgi.jps

import com.github.bobi.osgiannotationprocessor.jps.OSGIScrBuilder
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jps.incremental.messages.BuildMessage
import org.jetbrains.jps.model.java.JpsJavaLibraryType
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.util.JpsPathUtil
import java.io.File

class OsgiBuildTest : OsgiBuildTestCase() {
    private lateinit var myModule: JpsModule

    private val serviceInterfaceSrc = """
        package a;
        public interface Service {
            void foo();
        }
  """.trimIndent()

    private val serviceSrc = """
        package a.impl;
        
        import a.Service;
        import org.osgi.service.component.annotations.Component;
        
        @Component(service = Service.class, immediate = true)
        public class ServiceImpl implements Service {
        
            @Override
            public void foo() {
                System.out.println("foo");    
            }
        }
  """.trimIndent()

    override fun setUp() {
        super.setUp()
        myModule = module("main")

        val library = myModule.addModuleLibrary("osgi-annotations", JpsJavaLibraryType.INSTANCE)

        library.addRoot(
            JpsPathUtil.pathToUrl(
                FileUtil.toSystemIndependentName(
                    File("src/test/resources/libs/org.osgi.service.component.annotations-1.4.0.jar").canonicalPath
                )
            ),
            JpsOrderRootType.COMPILED
        )

        myModule.dependenciesList.addLibraryDependency(library)
    }

    fun testGenerateXml() {
        createFile("main/src/main/a/Service.java", serviceInterfaceSrc)
        createFile("main/src/main/a/impl/ServiceImpl.java", serviceSrc)
        val buildResult = buildAllModules()
        buildResult.assertSuccessful()

        buildResult.getMessages(BuildMessage.Kind.INFO).forEach { println(it.messageText) }

        assertCompiled(OSGIScrBuilder.ID, "${myModule.name}/out/OSGI-INF/a.impl.ServiceImpl.xml")

        println(File(orCreateProjectDir, "${myModule.name}/out/OSGI-INF/a.impl.ServiceImpl.xml").readText())
    }
}