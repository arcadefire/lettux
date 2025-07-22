package io.github.arcadefire.lettuce.android

import android.util.Log
import io.github.arcadefire.lettuce.LogsWriter

object AndroidLogsWriter : LogsWriter {
    override fun writeLog(message: String) {
        Log.d("LoggerMiddleware", message)
    }
}