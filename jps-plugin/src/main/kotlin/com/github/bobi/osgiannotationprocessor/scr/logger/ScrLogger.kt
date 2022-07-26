package com.github.bobi.osgiannotationprocessor.scr.logger

interface ScrLogger {

    val isDebugEnabled: Boolean

    fun debug(msg: String? = null, location: SourceLocation? = null)

    fun debug(ex: Throwable? = null, msg: String? = null)

    val isInfoEnabled: Boolean

    fun info(msg: String? = null, location: SourceLocation? = null)

    fun info(ex: Throwable? = null, msg: String? = null)

    val isWarnEnabled: Boolean

    fun warn(msg: String? = null, location: SourceLocation? = null)

    fun warn(ex: Throwable? = null, msg: String? = null)

    val hasErrors: Boolean

    val isErrorEnabled: Boolean

    fun error(msg: String? = null, location: SourceLocation? = null)

    fun error(ex: Throwable? = null, msg: String? = null)
}

class SourceLocation(val path: String? = null, val line: Int = -1, val col: Int = -1)