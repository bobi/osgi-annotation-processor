package com.github.bobi.osgiannotationprocessor.scr.processor

import java.io.File

/**
 * User: Andrey Bardashevsky
 * Date/Time: 25.07.2022 23:03
 */
data class ExecutionResult(val success: Boolean, val generatedFiles: Collection<File> = listOf())
