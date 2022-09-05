// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.osgi.jps.org.jetbrains.jps.builders

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ObjectUtils
import gnu.trove.TIntObjectHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntConsumer
import org.jetbrains.jps.builders.BuildTarget
import org.jetbrains.jps.cmdline.ProjectDescriptor
import org.jetbrains.jps.incremental.MessageHandler
import org.jetbrains.jps.incremental.messages.BuildMessage
import org.jetbrains.jps.incremental.messages.DoneSomethingNotification
import org.junit.Assert
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class BuildResult : MessageHandler {
    private val myErrorMessages: MutableList<BuildMessage> = mutableListOf()
    private val myWarnMessages: MutableList<BuildMessage> = mutableListOf()
    private val myInfoMessages: MutableList<BuildMessage> = mutableListOf()

    private var myUpToDate = true

    var mappingsDump: String? = null
        private set

    fun storeMappingsDump(pd: ProjectDescriptor) {
        mappingsDump = ByteArrayOutputStream().use { dump ->

            PrintStream(dump).use { stream ->
                pd.dataManager.mappings.toStream(stream)

                dumpSourceToOutputMappings(pd, stream)
            }

            return@use dump.toString()
        }
    }

    override fun processMessage(msg: BuildMessage) {
        when (msg.kind) {
            BuildMessage.Kind.ERROR -> {
                myErrorMessages.add(msg)
                myUpToDate = false
            }

            BuildMessage.Kind.WARNING -> {
                myWarnMessages.add(msg)
            }

            else -> {
                myInfoMessages.add(msg)
            }
        }
        if (msg is DoneSomethingNotification) {
            myUpToDate = false
        }
    }

    fun assertUpToDate() {
        Assert.assertTrue("Project sources weren't up to date", myUpToDate)
    }

    fun assertFailed() {
        Assert.assertFalse("Build not failed as expected", isSuccessful)
    }

    private val isSuccessful: Boolean
        get() = myErrorMessages.isEmpty()

    fun assertSuccessful() {
        if (!isSuccessful) {
            Assert.fail(
                """
    Build failed.
    Errors:
    ${StringUtil.join(myErrorMessages, "\n")}
    Info messages:
    ${StringUtil.join(myInfoMessages, "\n")}
    """.trimIndent()
            )
        }
    }

    fun getMessages(kind: BuildMessage.Kind): List<BuildMessage> {
        return when (kind) {
            BuildMessage.Kind.ERROR -> {
                myErrorMessages
            }

            BuildMessage.Kind.WARNING -> {
                myWarnMessages
            }

            else -> {
                myInfoMessages
            }
        }
    }

    companion object {
        private fun dumpSourceToOutputMappings(pd: ProjectDescriptor, stream: PrintStream) {
            val targets = pd.buildTargetIndex.allTargets.toMutableList()

            targets.sortWith { o1: BuildTarget<*>, o2: BuildTarget<*> ->
                StringUtil.comparePairs(
                    o1.targetType.typeId,
                    o1.id,
                    o2.targetType.typeId,
                    o2.id,
                    false
                )
            }

            val id2Target: Int2ObjectMap<BuildTarget<*>> = Int2ObjectOpenHashMap()
            for (target in targets) {
                id2Target.put(pd.dataManager.targetsState.getBuildTargetId(target), target)
            }
            val hashCodeToOutputPath = TIntObjectHashMap<String>()
            for (target in targets) {
                stream.println("Begin Of SourceToOutput (target " + getTargetIdWithTypeId(target) + ")")
                val map = pd.dataManager.getSourceToOutputMap(target)
                val sourcesList = map.sources.toMutableList()

                sourcesList.sort()

                for (source in sourcesList) {
                    val outputs = ObjectUtils.notNull(map.getOutputs(source), emptySet()).toMutableList()

                    outputs.sort()

                    for (output in outputs) {
                        hashCodeToOutputPath.put(FileUtil.pathHashCode(output), output)
                    }
                    val sourceToCompare = if (SystemInfo.isFileSystemCaseSensitive) source else source.lowercase()
                    stream.println(" " + sourceToCompare + " -> " + StringUtil.join(outputs, ","))
                }
                stream.println("End Of SourceToOutput (target " + getTargetIdWithTypeId(target) + ")")
            }
            val registry = pd.dataManager.outputToTargetRegistry
            val keys = registry.keys.toMutableList()
            keys.sort()

            stream.println("Begin Of OutputToTarget")
            for (key in keys) {
                val targetsIds = registry.getState(key) ?: continue
                val targetsNames: MutableList<String> = ArrayList()
                targetsIds.forEach(IntConsumer { value: Int ->
                    val target = id2Target[value]
                    targetsNames.add(if (target != null) getTargetIdWithTypeId(target) else "<unknown $value>")
                })

                targetsNames.sort()

                stream.println(hashCodeToOutputPath[key] + " -> " + targetsNames)
            }
            stream.println("End Of OutputToTarget")
        }

        private fun getTargetIdWithTypeId(target: BuildTarget<*>): String {
            return target.targetType.typeId + ":" + target.id
        }
    }
}