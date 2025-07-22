package io.github.arcadefire.lettuce

fun interface LogsWriter {
    fun writeLog(message: String)
}
