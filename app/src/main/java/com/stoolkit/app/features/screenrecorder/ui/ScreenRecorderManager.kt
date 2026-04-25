package com.stoolkit.app.features.screenrecorder.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.MediaProjection
import android.media.MediaProjectionManager
import android.os.Build
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blind_tech_nexus.app.features.screenrecorder.handler.*
import com.blind_tech_nexus.app.features.screenrecorder.model.RecordingEvent
import com.blind_tech_nexus.app.features.screenrecorder.model.RecordingState
import com.blind_tech_nexus.app.features.screenrecorder.model.ScreenRecordingConfig
import com.blind_tech_nexus.app.features.screenrecorder.service.ScreenRecorderForegroundService
import java.io.File

/**
 * Main manager class for screen recording functionality
 * Coordinates all handlers and manages the recording lifecycle
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenRecorderManager(
    private val context: Context,
    private val config: ScreenRecordingConfig = ScreenRecordingConfig()
) : ScreenRecorderServiceManager {
    
    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    
    private var mediaRecorderHandler: MediaRecorderHandler? = null
    private var virtualDisplayHandler: VirtualDisplayHandler? = null
    private var notificationHandler: NotificationHandler? = null
    private var permissionHandler: PermissionHandler? = null
    
    private val _recordingState = MutableLiveData<RecordingState>()
    private val _recordingEvents = MutableLiveData<RecordingEvent>()
    
    val recordingState: LiveData<RecordingState> = _recordingState
    val recordingEvents: LiveData<RecordingEvent> = _recordingEvents
    
    private var currentOutputPath: String? = null
    private var recordingStartTime: Long = 0L
    
    companion object {
        const val REQUEST_CODE_MEDIA_PROJECTION = 10001
    }
    
    init {
        initializeHandlers()
    }
    
    /**
     * Initialize all handler instances
     */
    private fun initializeHandlers() {
        notificationHandler = NotificationHandler(context)
        permissionHandler = PermissionHandler(context)
        
        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationHandler?.createNotificationChannel()
        }
    }
    
    /**
     * Get MediaProjectionManager instance
     */
    fun getMediaProjectionManager(): MediaProjectionManager {
        if (mediaProjectionManager == null) {
            mediaProjectionManager = context.getSystemService(
                Activity.MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager
        }
        return mediaProjectionManager!!
    }
    
    /**
     * Create MediaProjection intent for permission request
     */
    fun createMediaProjectionIntent(): Intent {
        return getMediaProjectionManager().createScreenCaptureIntent()
    }
    
    /**
     * Handle MediaProjection permission result
     */
    fun handleMediaProjectionResult(resultCode: Int, data: Intent?): Boolean {
        if (resultCode != Activity.RESULT_OK || data == null) {
            _recordingEvents.value = RecordingEvent.RecordingError("Media projection permission denied")
            return false
        }
        
        try {
            mediaProjection = getMediaProjectionManager().getMediaProjection(resultCode, data)
            return true
        } catch (e: Exception) {
            _recordingEvents.value = RecordingEvent.RecordingError("Failed to get media projection: ${e.message}")
            return false
        }
    }
    
    /**
     * Start screen recording
     */
    fun startRecording(): Boolean {
        if (mediaProjection == null) {
            _recordingEvents.value = RecordingEvent.RecordingError("Media projection not initialized")
            return false
        }
        
        try {
            // Generate output file path
            currentOutputPath = permissionHandler?.generateOutputFilePath()
            
            if (currentOutputPath.isNullOrEmpty()) {
                _recordingEvents.value = RecordingEvent.RecordingError("Failed to generate output path")
                return false
            }
            
            // Initialize MediaRecorder
            val outputFile = File(currentOutputPath!!)
            mediaRecorderHandler = MediaRecorderHandler(config, outputFile)
            
            if (!mediaRecorderHandler?.initialize()!!) {
                _recordingEvents.value = RecordingEvent.RecordingError("Failed to initialize media recorder")
                return false
            }
            
            // Get input surface from MediaRecorder
            val inputSurface = mediaRecorderHandler?.getInputSurface()
            
            if (inputSurface == null) {
                _recordingEvents.value = RecordingEvent.RecordingError("Failed to get input surface")
                return false
            }
            
            // Create virtual display
            virtualDisplayHandler = VirtualDisplayHandler(mediaProjection!!, config)
            
            if (!virtualDisplayHandler?.createVirtualDisplay(inputSurface)!!) {
                _recordingEvents.value = RecordingEvent.RecordingError("Failed to create virtual display")
                return false
            }
            
            // Start recording
            mediaRecorderHandler?.startRecording()
            
            // Start foreground service
            val serviceIntent = Intent(context, ScreenRecorderForegroundService::class.java).apply {
                action = ScreenRecorderForegroundService.ACTION_START_RECORDING
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            // Update state
            _recordingState.value = RecordingState.Recording
            _recordingEvents.value = RecordingEvent.RecordingStarted
            recordingStartTime = System.currentTimeMillis()
            
            return true
        } catch (e: Exception) {
            _recordingEvents.value = RecordingEvent.RecordingError("Failed to start recording: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Pause screen recording
     */
    fun pauseRecording() {
        if (_recordingState.value != RecordingState.Recording) {
            return
        }
        
        try {
            // Pause MediaRecorder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorderHandler?.pauseRecording()
            }
            
            // Pause foreground service
            val serviceIntent = Intent(context, ScreenRecorderForegroundService::class.java).apply {
                action = ScreenRecorderForegroundService.ACTION_PAUSE_RECORDING
            }
            context.startService(serviceIntent)
            
            // Update state
            _recordingState.value = RecordingState.Paused
            _recordingEvents.value = RecordingEvent.RecordingPaused
        } catch (e: Exception) {
            _recordingEvents.value = RecordingEvent.RecordingError("Failed to pause recording: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Resume screen recording
     */
    fun resumeRecording() {
        if (_recordingState.value != RecordingState.Paused) {
            return
        }
        
        try {
            // Resume MediaRecorder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorderHandler?.resumeRecording()
            }
            
            // Resume foreground service
            val serviceIntent = Intent(context, ScreenRecorderForegroundService::class.java).apply {
                action = ScreenRecorderForegroundService.ACTION_RESUME_RECORDING
            }
            context.startService(serviceIntent)
            
            // Update state
            _recordingState.value = RecordingState.Recording
            _recordingEvents.value = RecordingEvent.RecordingResumed
        } catch (e: Exception) {
            _recordingEvents.value = RecordingEvent.RecordingError("Failed to resume recording: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Stop screen recording
     */
    fun stopRecording() {
        if (_recordingState.value == RecordingState.Idle || 
            _recordingState.value == RecordingState.Stopped) {
            return
        }
        
        try {
            // Stop MediaRecorder
            mediaRecorderHandler?.stopRecording()
            
            // Stop foreground service
            val serviceIntent = Intent(context, ScreenRecorderForegroundService::class.java).apply {
                action = ScreenRecorderForegroundService.ACTION_STOP_RECORDING
            }
            context.startService(serviceIntent)
            
            // Release resources
            releaseResources()
            
            // Calculate duration
            val duration = (System.currentTimeMillis() - recordingStartTime) / 1000
            
            // Update state
            _recordingState.value = RecordingState.Stopped
            _recordingEvents.value = RecordingEvent.RecordingStopped
            
            // Notify about saved recording
            currentOutputPath?.let { path ->
                _recordingEvents.value = RecordingEvent.RecordingSaved(path)
                onRecordingSaved(path)
                
                // Show completion notification
                notificationHandler?.showRecordingCompletedNotification(path)
            }
        } catch (e: Exception) {
            _recordingEvents.value = RecordingEvent.RecordingError("Failed to stop recording: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Release all recording resources
     */
    private fun releaseResources() {
        try {
            virtualDisplayHandler?.release()
            mediaRecorderHandler?.release()
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean {
        return _recordingState.value == RecordingState.Recording ||
               _recordingState.value == RecordingState.Paused
    }
    
    /**
     * Get current recording configuration
     */
    fun getConfig(): ScreenRecordingConfig {
        return config
    }
    
    // ScreenRecorderServiceManager callbacks
    
    override fun onRecordingStarted() {
        // Service started successfully
    }
    
    override fun onRecordingPaused() {
        // Service paused
    }
    
    override fun onRecordingResumed() {
        // Service resumed
    }
    
    override fun onRecordingStopped() {
        // Service stopped
    }
    
    override fun onRecordingError(message: String) {
        _recordingEvents.value = RecordingEvent.RecordingError(message)
    }
    
    override fun onRecordingSaved(filePath: String) {
        // Recording saved successfully
    }
    
    /**
     * Cleanup when manager is no longer needed
     */
    fun cleanup() {
        if (isRecording()) {
            stopRecording()
        }
        releaseResources()
    }
}
