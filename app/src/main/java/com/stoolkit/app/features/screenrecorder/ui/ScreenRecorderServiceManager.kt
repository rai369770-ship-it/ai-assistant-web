package com.stoolkit.app.features.screenrecorder.ui

/**
 * Interface for managing screen recorder service callbacks
 */
interface ScreenRecorderServiceManager {
    /**
     * Called when recording starts
     */
    fun onRecordingStarted()
    
    /**
     * Called when recording is paused
     */
    fun onRecordingPaused()
    
    /**
     * Called when recording is resumed
     */
    fun onRecordingResumed()
    
    /**
     * Called when recording stops
     */
    fun onRecordingStopped()
    
    /**
     * Called when an error occurs during recording
     */
    fun onRecordingError(message: String)
    
    /**
     * Called when recording is saved successfully
     */
    fun onRecordingSaved(filePath: String)
}
