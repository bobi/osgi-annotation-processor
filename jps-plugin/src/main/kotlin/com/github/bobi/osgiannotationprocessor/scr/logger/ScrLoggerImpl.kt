package com.github.bobi.osgiannotationprocessor.scr.logger

import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.messages.BuildMessage
import org.jetbrains.jps.incremental.messages.CompilerMessage
import java.util.logging.Level
import java.util.logging.Logger

class ScrLoggerImpl(
    private val context: CompileContext,
    private val compilerName: String,
    private val moduleName: String,
    private val debugLogging: Boolean
) : ScrLogger {
    private val logger = Logger.getLogger(javaClass.name)

    override var hasErrors: Boolean = false

    override val isInfoEnabled: Boolean = true
    override val isWarnEnabled: Boolean = true
    override val isErrorEnabled: Boolean = true
    override val isDebugEnabled: Boolean
        get() = debugLogging

    override fun info(ex: Throwable?, msg: String?) {
        logImpl(ScrLogLevel.INFO, msg, ex)
    }

    override fun info(msg: String?, location: SourceLocation?) {
        logImpl(ScrLogLevel.INFO, msg, null, location)
    }

    override fun error(ex: Throwable?, msg: String?) {
        hasErrors = true
        logImpl(ScrLogLevel.ERROR, msg, ex)
    }

    override fun error(msg: String?, location: SourceLocation?) {
        hasErrors = true
        logImpl(ScrLogLevel.ERROR, msg, null, location)
    }

    override fun warn(ex: Throwable?, msg: String?) {
        logImpl(ScrLogLevel.WARN, msg, ex)
    }

    override fun warn(msg: String?, location: SourceLocation?) {
        logImpl(ScrLogLevel.WARN, msg, null, location)
    }

    override fun debug(msg: String?, location: SourceLocation?) {
        Logger.getLogger(javaClass.name).log(Level.FINE, msg)
        if (debugLogging) {
            info(msg, location)
        }
    }

    override fun debug(ex: Throwable?, msg: String?) {
        Logger.getLogger(javaClass.name).log(Level.FINE, msg, ex)
        if (debugLogging) {
            info(ex, msg)
        }
    }

    private fun logImpl(l: ScrLogLevel, message: String?, t: Throwable?, location: SourceLocation? = null) {
        log(l, "[$moduleName] ${message ?: t?.message ?: ""}".trim(), t, location ?: SourceLocation())
    }

    private enum class ScrLogLevel {
        ERROR, WARN, INFO
    }

    private fun log(l: ScrLogLevel, message: String?, t: Throwable?, location: SourceLocation) {
        var kind: BuildMessage.Kind = BuildMessage.Kind.ERROR
        var jl = Level.SEVERE

        when (l) {
            ScrLogLevel.ERROR -> {
                kind = BuildMessage.Kind.ERROR
                jl = Level.SEVERE
            }
            ScrLogLevel.WARN -> {
                kind = BuildMessage.Kind.WARNING
                jl = Level.WARNING
            }
            ScrLogLevel.INFO -> {
                kind = BuildMessage.Kind.INFO
                jl = Level.INFO
            }
        }

        context.processMessage(
            CompilerMessage(
                compilerName,
                kind,
                message,
                location.path,
                location.line.toLong(),
                location.line.toLong(),
                location.line.toLong(),
                location.line.toLong(),
                location.col.toLong()
            )
        )

        logger.log(jl, message, t)
    }
}