package io.github.arcadefire.lettuce.android.logger

import android.util.Log

fun interface LogsWriter {
    fun writeLog(message: String)
}

object AndroidLogsWriter : LogsWriter {
    override fun writeLog(message: String) {
        Log.d("LoggerMiddleware", message)
    }
}
