package com.stoolkit.app.features.screenrecorder.model

/**
 * Data class representing screen recording configuration options
 */
data class ScreenRecordingConfig(
    val enableMicrophone: Boolean = false,
    val enableDeviceAudio: Boolean = true,
    val showTouches: Boolean = true,
    val videoWidth: Int = 1080,
    val videoHeight: Int = 1920,
    val videoDpi: Int = 320,
    val videoFrameRate: Int = 30,
    val videoBitRate: Int = 6_000_000
)

/**
 * Sealed class representing the current state of screen recording
 */
sealed class RecordingState {
    object Idle : RecordingState()
    object Recording : RecordingState()
    object Paused : RecordingState()
    object Stopped : RecordingState()
}

/**
 * Data class representing a saved screen recording file
 */
data class ScreenRecordingFile(
    val filePath: String,
    val fileName: String,
    val createdAt: Long,
    val duration: Long = 0L,
    val fileSize: Long = 0L
) {
    val formattedDate: String
        get() {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            return dateFormat.format(java.util.Date(createdAt))
        }
    
    val formattedDuration: String
        get() {
            val hours = duration / 3600
            val minutes = (duration % 3600) / 60
            val seconds = duration % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
}

/**
 * sealed class for recording events
 */
sealed class RecordingEvent {
    object RecordingStarted : RecordingEvent()
    object RecordingPaused : RecordingEvent()
    object RecordingResumed : RecordingEvent()
    object RecordingStopped : RecordingEvent()
    data class RecordingError(val message: String) : RecordingEvent()
    data class RecordingSaved(val filePath: String) : RecordingEvent()
}
