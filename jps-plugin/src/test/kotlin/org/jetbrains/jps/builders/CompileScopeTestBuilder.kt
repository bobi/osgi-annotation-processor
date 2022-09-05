// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.builders

import com.intellij.util.containers.FileCollectionFactory
import com.intellij.util.containers.SmartHashSet
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType
import org.jetbrains.jps.incremental.CompileScope
import org.jetbrains.jps.incremental.CompileScopeImpl
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.artifacts.ArtifactBuildTarget
import org.jetbrains.jps.incremental.artifacts.ArtifactBuildTargetType
import org.jetbrains.jps.model.artifact.JpsArtifact
import org.jetbrains.jps.model.module.JpsModule
import java.io.File

class CompileScopeTestBuilder private constructor(private val myForceBuild: Boolean) {
    private val myTargetTypes: MutableSet<BuildTargetType<*>> = HashSet()
    private val myTargets: MutableSet<BuildTarget<*>> = HashSet()
    private val myFiles = LinkedHashMap<BuildTarget<*>, MutableSet<File>>()
    fun allModules(): CompileScopeTestBuilder {
        myTargetTypes.addAll(JavaModuleBuildTargetType.ALL_TYPES)
        return this
    }

    fun module(module: JpsModule?): CompileScopeTestBuilder {
        myTargets.add(ModuleBuildTarget(module!!, JavaModuleBuildTargetType.PRODUCTION))
        myTargets.add(ModuleBuildTarget(module, JavaModuleBuildTargetType.TEST))
        return this
    }

    fun allArtifacts(): CompileScopeTestBuilder {
        myTargetTypes.add(ArtifactBuildTargetType.INSTANCE)
        return this
    }

    fun artifact(artifact: JpsArtifact?): CompileScopeTestBuilder {
        myTargets.add(ArtifactBuildTarget(artifact!!))
        return this
    }

    fun targetTypes(vararg targets: BuildTargetType<*>): CompileScopeTestBuilder {
        myTargetTypes.addAll(listOf(*targets))
        return this
    }

    fun file(target: BuildTarget<*>, path: String?): CompileScopeTestBuilder {
        var files = myFiles[target]
        if (files == null) {
            files = FileCollectionFactory.createCanonicalFileSet()
            myFiles[target] = files
        }
        files.add(File(path))
        return this
    }

    fun build(): CompileScope {
        val typesToForceBuild: Collection<BuildTargetType<*>>
        if (myForceBuild) {
            typesToForceBuild = SmartHashSet()
            typesToForceBuild.addAll(myTargetTypes)
            for (target in myTargets) {
                typesToForceBuild.add(target.targetType)
            }
        } else {
            typesToForceBuild = emptyList()
        }
        return CompileScopeImpl(myTargetTypes, typesToForceBuild, myTargets, myFiles)
    }

    fun artifacts(vararg artifacts: JpsArtifact?): CompileScopeTestBuilder {
        for (artifact in artifacts) {
            myTargets.add(ArtifactBuildTarget(artifact!!))
        }
        return this
    }

    companion object {
        fun rebuild(): CompileScopeTestBuilder {
            return CompileScopeTestBuilder(true)
        }

        fun make(): CompileScopeTestBuilder {
            return CompileScopeTestBuilder(false)
        }

        fun recompile(): CompileScopeTestBuilder {
            return CompileScopeTestBuilder(true)
        }
    }
}