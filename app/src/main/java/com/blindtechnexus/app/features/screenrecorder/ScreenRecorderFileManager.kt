package com.blindtechnexus.app.features.screenrecorder

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScreenRecorderFileManager {
    private const val RECORDING_ROOT = "blind-tech-nexus/screen-recordings"

    fun createOutputFile(context: Context): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: context.filesDir
        val outputDir = File(baseDir, RECORDING_ROOT)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val time = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val filename = "screen-recording_$time.mp4"
        return File(outputDir, filename)
    }

    fun latestRecordingFile(context: Context): File? {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: context.filesDir
        val outputDir = File(baseDir, RECORDING_ROOT)
        return outputDir.listFiles()?.filter { it.isFile }?.maxByOrNull { it.lastModified() }
    }
}