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
import androidx.lifecycle.viewModelScope
import com.blind_tech_nexus.app.features.screenrecorder.handler.*
import com.blind_tech_nexus.app.features.screenrecorder.model.RecordingEvent
import com.blind_tech_nexus.app.features.screenrecorder.model.RecordingState
import com.blind_tech_nexus.app.features.screenrecorder.model.ScreenRecordingConfig
import com.blind_tech_nexus.app.features.screenrecorder.service.ScreenRecorderForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for Screen Recorder screen
 * Manages UI state and coordinates with ScreenRecorderManager
 */
class ScreenRecorderViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    
    private val context = application.applicationContext
    
    // Handlers
    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaRecorderHandler: MediaRecorderHandler? = null
    private var virtualDisplayHandler: VirtualDisplayHandler? = null
    private var floatOverlayHandler: FloatOverlayHandler? = null
    private var notificationHandler: NotificationHandler? = null
    val permissionHandler = PermissionHandler(context)
    
    // UI State
    private val _uiState = MutableStateFlow(ScreenRecorderUiState())
    val uiState: StateFlow<ScreenRecorderUiState> = _uiState.asStateFlow()
    
    // Recording state
    private val _recordingState = MutableLiveData<RecordingState>()
    val recordingState: LiveData<RecordingState> = _recordingState
    
    // Recording events
    private val _recordingEvents = MutableLiveData<RecordingEvent>()
    val recordingEvents: LiveData<RecordingEvent> = _recordingEvents
    
    private var currentOutputPath: String? = null
    private var recordingStartTime: Long = 0L
    
    init {
        initializeHandlers()
        checkPermissions()
    }
    
    /**
     * Initialize all handler instances
     */
    private fun initializeHandlers() {
        notificationHandler = NotificationHandler(context)
        
        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationHandler?.createNotificationChannel()
        }
        
        // Initialize float overlay handler
        floatOverlayHandler = FloatOverlayHandler(context)
    }
    
    /**
     * Get MediaProjectionManager instance
     */
    private fun getMediaProjectionManager(): MediaProjectionManager {
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
     * Check initial permissions
     */
    private fun checkPermissions() {
        _uiState.value = _uiState.value.copy(
            hasOverlayPermission = permissionHandler.hasOverlayPermission(),
            hasNotificationPermission = permissionHandler.hasNotificationAccess()
        )
    }
    
    /**
     * Check overlay permission status
     */
    fun checkOverlayPermission() {
        _uiState.value = _uiState.value.copy(
            hasOverlayPermission = permissionHandler.hasOverlayPermission()
        )
    }
    
    /**
     * Handle notification permission result
     */
    fun onNotificationPermissionResult(isGranted: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasNotificationPermission = isGranted
        )
    }
    
    /**
     * Toggle microphone setting
     */
    fun toggleMicrophone(enabled: Boolean) {
        val newConfig = _uiState.value.config.copy(enableMicrophone = enabled)
        _uiState.value = _uiState.value.copy(config = newConfig)
    }
    
    /**
     * Toggle device audio setting
     */
    fun toggleDeviceAudio(enabled: Boolean) {
        val newConfig = _uiState.value.config.copy(enableDeviceAudio = enabled)
        _uiState.value = _uiState.value.copy(config = newConfig)
    }
    
    /**
     * Toggle show touches setting
     */
    fun toggleShowTouches(enabled: Boolean) {
        val newConfig = _uiState.value.config.copy(showTouches = enabled)
        _uiState.value = _uiState.value.copy(config = newConfig)
    }
    
    /**
     * Handle MediaProjection permission result
     */
    fun handleMediaProjectionResult(resultCode: Int, data: Intent?) {
        viewModelScope.launch {
            if (resultCode != Activity.RESULT_OK || data == null) {
                _recordingEvents.value = RecordingEvent.RecordingError("Media projection permission denied")
                return@launch
            }
            
            try {
                mediaProjection = getMediaProjectionManager().getMediaProjection(resultCode, data)
                // Start recording after permission granted
                startRecording()
            } catch (e: Exception) {
                _recordingEvents.value = RecordingEvent.RecordingError("Failed to get media projection: ${e.message}")
            }
        }
    }
    
    /**
     * Start screen recording
     */
    private fun startRecording() {
        viewModelScope.launch {
            if (mediaProjection == null) {
                _recordingEvents.value = RecordingEvent.RecordingError("Media projection not initialized")
                return@launch
            }
            
            try {
                // Generate output file path
                currentOutputPath = permissionHandler.generateOutputFilePath()
                
                if (currentOutputPath.isNullOrEmpty()) {
                    _recordingEvents.value = RecordingEvent.RecordingError("Failed to generate output path")
                    return@launch
                }
                
                // Initialize MediaRecorder
                val outputFile = File(currentOutputPath!!)
                mediaRecorderHandler = MediaRecorderHandler(_uiState.value.config, outputFile)
                
                if (!mediaRecorderHandler?.initialize()!!) {
                    _recordingEvents.value = RecordingEvent.RecordingError("Failed to initialize media recorder")
                    return@launch
                }
                
                // Get input surface from MediaRecorder
                val inputSurface = mediaRecorderHandler?.getInputSurface()
                
                if (inputSurface == null) {
                    _recordingEvents.value = RecordingEvent.RecordingError("Failed to get input surface")
                    return@launch
                }
                
                // Create virtual display
                virtualDisplayHandler = VirtualDisplayHandler(mediaProjection!!, _uiState.value.config)
                
                if (!virtualDisplayHandler?.createVirtualDisplay(inputSurface)!!) {
                    _recordingEvents.value = RecordingEvent.RecordingError("Failed to create virtual display")
                    return@launch
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
                
                // Show floating overlay
                showFloatingOverlay()
                
                // Update state
                _recordingState.value = RecordingState.Recording
                _recordingEvents.value = RecordingEvent.RecordingStarted
                recordingStartTime = System.currentTimeMillis()
                
            } catch (e: Exception) {
                _recordingEvents.value = RecordingEvent.RecordingError("Failed to start recording: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Show floating overlay with controls
     */
    private fun showFloatingOverlay() {
        floatOverlayHandler?.showFloatingOverlay(
            state = RecordingState.Recording,
            onPause = { pauseRecording() },
            onStop = { stopRecording() },
            onResume = { resumeRecording() }
        )
    }
    
    /**
     * Update floating overlay state
     */
    private fun updateFloatingOverlay(state: RecordingState) {
        floatOverlayHandler?.updateOverlayState(state)
    }
    
    /**
     * Hide floating overlay
     */
    private fun hideFloatingOverlay() {
        floatOverlayHandler?.hideFloatingOverlay()
    }
    
    /**
     * Pause screen recording
     */
    fun pauseRecording() {
        if (_recordingState.value != RecordingState.Recording) {
            return
        }
        
        viewModelScope.launch {
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
                
                // Update overlay
                updateFloatingOverlay(RecordingState.Paused)
                
                // Update state
                _recordingState.value = RecordingState.Paused
                _recordingEvents.value = RecordingEvent.RecordingPaused
            } catch (e: Exception) {
                _recordingEvents.value = RecordingEvent.RecordingError("Failed to pause recording: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Resume screen recording
     */
    fun resumeRecording() {
        if (_recordingState.value != RecordingState.Paused) {
            return
        }
        
        viewModelScope.launch {
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
                
                // Update overlay
                updateFloatingOverlay(RecordingState.Recording)
                
                // Update state
                _recordingState.value = RecordingState.Recording
                _recordingEvents.value = RecordingEvent.RecordingResumed
            } catch (e: Exception) {
                _recordingEvents.value = RecordingEvent.RecordingError("Failed to resume recording: ${e.message}")
                e.printStackTrace()
            }
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
        
        viewModelScope.launch {
            try {
                // Stop MediaRecorder
                mediaRecorderHandler?.stopRecording()
                
                // Stop foreground service
                val serviceIntent = Intent(context, ScreenRecorderForegroundService::class.java).apply {
                    action = ScreenRecorderForegroundService.ACTION_STOP_RECORDING
                }
                context.startService(serviceIntent)
                
                // Hide floating overlay
                hideFloatingOverlay()
                
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
                    
                    // Show completion notification
                    notificationHandler?.showRecordingCompletedNotification(path)
                }
            } catch (e: Exception) {
                _recordingEvents.value = RecordingEvent.RecordingError("Failed to stop recording: ${e.message}")
                e.printStackTrace()
            }
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
     * Handle permission denied
     */
    fun onPermissionDenied(message: String) {
        _uiState.value = _uiState.value.copy(
            currentEvent = RecordingEvent.RecordingError(message)
        )
    }
    
    /**
     * Delete a recording file
     */
    fun deleteRecording(filePath: String) {
        viewModelScope.launch {
            val success = permissionHandler.deleteRecording(filePath)
            if (success) {
                // Refresh recordings list if needed
            }
        }
    }
    
    /**
     * Get all saved recordings
     */
    fun getSavedRecordings(): List<com.blind_tech_nexus.app.features.screenrecorder.model.ScreenRecordingFile> {
        return permissionHandler.getSavedRecordings()
    }
    
    override fun onCleared() {
        super.onCleared()
        if (_recordingState.value == RecordingState.Recording || 
            _recordingState.value == RecordingState.Paused) {
            stopRecording()
        }
        floatOverlayHandler?.cleanup()
    }
}

/**
 * UI State data class for Screen Recorder
 */
data class ScreenRecorderUiState(
    val config: ScreenRecordingConfig = ScreenRecordingConfig(),
    val hasOverlayPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val currentEvent: RecordingEvent? = null,
    val lastSavedPath: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
