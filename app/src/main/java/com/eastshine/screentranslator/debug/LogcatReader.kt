package com.eastshine.screentranslator.debug

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Reads logcat output in real-time
 */
object LogcatReader {
    private const val TAG = "LogcatReader"

    /**
     * Returns a Flow that continuously streams log lines
     * Filters by tags specified in DebugConfig.logTags
     */
    fun streamLogs(): Flow<String> =
        flow {
            try {
                // Clear existing logs and start streaming
                Runtime.getRuntime().exec("logcat -c").waitFor()

                // Build command with tag filtering
                // logcat -v brief -s Tag1 Tag2 Tag3
                val command =
                    mutableListOf("logcat", "-v", "brief", "-s").apply {
                        addAll(DebugConfig.logTags)
                    }

                val process = Runtime.getRuntime().exec(command.toTypedArray())

                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))

                // Continuously read and emit log lines
                bufferedReader.useLines { sequence ->
                    sequence.forEach { line ->
                        emit(line)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stream logcat", e)
                emit("Error streaming logs: ${e.message}")
            }
        }.flowOn(Dispatchers.IO)
}
