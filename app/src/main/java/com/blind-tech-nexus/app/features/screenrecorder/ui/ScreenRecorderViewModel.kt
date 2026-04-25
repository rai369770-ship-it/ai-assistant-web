package com.blind_tech_nexus.app.features.screenrecorder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blind_tech_nexus.app.features.screenrecorder.handler.PermissionHandler
import com.blind_tech_nexus.app.features.screenrecorder.model.RecordingEvent
import com.blind_tech_nexus.app.features.screenrecorder.model.RecordingState
import com.blind_tech_nexus.app.features.screenrecorder.model.ScreenRecordingConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Screen Recorder screen
 * Manages UI state and coordinates with ScreenRecorderManager
 */
class ScreenRecorderViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    
    private var screenRecorderManager: ScreenRecorderManager? = null
    
    val permissionHandler = PermissionHandler(context)
    
    // UI State
    private val _uiState = MutableStateFlow(ScreenRecorderUiState())
    val uiState: StateFlow<ScreenRecorderUiState> = _uiState.asStateFlow()
    
    // Recording state from manager
    private val _recordingState = MutableLiveData<RecordingState>()
    val recordingState: LiveData<RecordingState> = _recordingState
    
    init {
        initializeRecorder()
        checkPermissions()
    }
    
    /**
     * Initialize the screen recorder manager
     */
    private fun initializeRecorder() {
        screenRecorderManager = ScreenRecorderManager(
            context = context,
            config = _uiState.value.config
        )
        
        // Observe recording state
        screenRecorderManager?.recordingState?.observeForever { state ->
            _recordingState.value = state
        }
        
        // Observe recording events
        screenRecorderManager?.recordingEvents?.observeForever { event ->
            _uiState.value = _uiState.value.copy(currentEvent = event)
            
            if (event is RecordingEvent.RecordingSaved) {
                _uiState.value = _uiState.value.copy(lastSavedPath = event.filePath)
            }
        }
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
        updateRecorderConfig(newConfig)
    }
    
    /**
     * Toggle device audio setting
     */
    fun toggleDeviceAudio(enabled: Boolean) {
        val newConfig = _uiState.value.config.copy(enableDeviceAudio = enabled)
        _uiState.value = _uiState.value.copy(config = newConfig)
        updateRecorderConfig(newConfig)
    }
    
    /**
     * Toggle show touches setting
     */
    fun toggleShowTouches(enabled: Boolean) {
        val newConfig = _uiState.value.config.copy(showTouches = enabled)
        _uiState.value = _uiState.value.copy(config = newConfig)
        updateRecorderConfig(newConfig)
    }
    
    /**
     * Update recorder configuration
     */
    private fun updateRecorderConfig(newConfig: ScreenRecordingConfig) {
        screenRecorderManager?.cleanup()
        initializeRecorder()
    }
    
    /**
     * Create MediaProjection intent for permission request
     */
    fun createMediaProjectionIntent(): android.content.Intent {
        return screenRecorderManager?.createMediaProjectionIntent() 
            ?: android.content.Intent()
    }
    
    /**
     * Handle MediaProjection permission result
     */
    fun handleMediaProjectionResult(resultCode: Int, data: android.content.Intent?) {
        viewModelScope.launch {
            val success = screenRecorderManager?.handleMediaProjectionResult(resultCode, data) == true
            
            if (success) {
                // Start recording after permission granted
                startRecording()
            } else {
                onPermissionDenied("Media projection permission denied")
            }
        }
    }
    
    /**
     * Start screen recording
     */
    private fun startRecording() {
        viewModelScope.launch {
            val success = screenRecorderManager?.startRecording() == true
            if (!success) {
                _uiState.value = _uiState.value.copy(
                    currentEvent = RecordingEvent.RecordingError("Failed to start recording")
                )
            }
        }
    }
    
    /**
     * Pause screen recording
     */
    fun pauseRecording() {
        screenRecorderManager?.pauseRecording()
    }
    
    /**
     * Resume screen recording
     */
    fun resumeRecording() {
        screenRecorderManager?.resumeRecording()
    }
    
    /**
     * Stop screen recording
     */
    fun stopRecording() {
        screenRecorderManager?.stopRecording()
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
    
    /**
     * Clear current event
     */
    fun clearCurrentEvent() {
        _uiState.value = _uiState.value.copy(currentEvent = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        screenRecorderManager?.cleanup()
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
