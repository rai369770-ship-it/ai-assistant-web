package com.stoolkit.app.features.screenrecorder.handler

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.blind_tech_nexus.app.features.screenrecorder.model.ScreenRecordingFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handler class for managing permissions required for screen recording
 */
class PermissionHandler(private val context: Context) {
    
    companion object {
        const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1002
    }
    
    /**
     * Check if display over other apps permission is granted
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    /**
     * Check if notification access is granted
     */
    fun hasNotificationAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+, check POST_NOTIFICATIONS permission
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    /**
     * Get intent to request overlay permission
     */
    fun getOverlayPermissionIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }
    
    /**
     * Get intent to request notification access
     */
    fun getNotificationAccessIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHandler.CHANNEL_ID)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
    }
    
    /**
     * Launch overlay permission request
     */
    fun launchOverlayPermissionRequest(launcher: ActivityResultLauncher<Intent>) {
        launcher.launch(getOverlayPermissionIntent())
    }
    
    /**
     * Launch notification access request
     */
    fun launchNotificationAccessRequest(launcher: ActivityResultLauncher<Intent>) {
        launcher.launch(getNotificationAccessIntent())
    }
    
    /**
     * Generate output file path for recording
     */
    fun generateOutputFilePath(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val fileName = "Screen_recording_$timestamp.mp4"
        
        // Create directory if it doesn't exist
        val recordingsDir = File(
            Environment.getExternalStorageDirectory(),
            "blind-tech-nexus/screen recordings"
        )
        
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }
        
        return File(recordingsDir, fileName).absolutePath
    }
    
    /**
     * Get all saved recording files
     */
    fun getSavedRecordings(): List<ScreenRecordingFile> {
        val recordingsDir = File(
            Environment.getExternalStorageDirectory(),
            "blind-tech-nexus/screen recordings"
        )
        
        val recordingsList = mutableListOf<ScreenRecordingFile>()
        
        if (recordingsDir.exists() && recordingsDir.isDirectory) {
            recordingsDir.listFiles { file ->
                file.isFile && file.extension.equals("mp4", ignoreCase = true)
            }?.forEach { file ->
                recordingsList.add(
                    ScreenRecordingFile(
                        filePath = file.absolutePath,
                        fileName = file.name,
                        createdAt = file.lastModified(),
                        fileSize = file.length()
                    )
                )
            }
        }
        
        // Sort by creation date (newest first)
        return recordingsList.sortedByDescending { it.createdAt }
    }
    
    /**
     * Delete a recording file
     */
    fun deleteRecording(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Share a recording file
     */
    fun getShareIntent(filePath: String): Intent {
        val file = File(filePath)
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }
        
        return Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
