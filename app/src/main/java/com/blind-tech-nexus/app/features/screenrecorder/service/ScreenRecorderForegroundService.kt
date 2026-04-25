package com.blind_tech_nexus.app.features.screenrecorder.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.blind_tech_nexus.app.features.screenrecorder.handler.NotificationHandler
import com.blind_tech_nexus.app.features.screenrecorder.model.RecordingState
import com.blind_tech_nexus.app.features.screenrecorder.ui.ScreenRecorderServiceManager

/**
 * Foreground service for handling screen recording in the background
 */
class ScreenRecorderForegroundService : Service() {
    
    companion object {
        const val ACTION_START_RECORDING = "com.blind_tech_nexus.action.START_RECORDING"
        const val ACTION_PAUSE_RECORDING = "com.blind_tech_nexus.action.PAUSE_RECORDING"
        const val ACTION_RESUME_RECORDING = "com.blind_tech_nexus.action.RESUME_RECORDING"
        const val ACTION_STOP_RECORDING = "com.blind_tech_nexus.action.STOP_RECORDING"
        
        private var isRunning = false
        private var serviceInstance: ScreenRecorderForegroundService? = null
        
        fun isServiceRunning(): Boolean = isRunning
        fun getServiceInstance(): ScreenRecorderForegroundService? = serviceInstance
    }
    
    private lateinit var notificationHandler: NotificationHandler
    private var currentState: RecordingState = RecordingState.Idle
    private var serviceManager: ScreenRecorderServiceManager? = null
    
    override fun onCreate() {
        super.onCreate()
        notificationHandler = NotificationHandler(this)
        serviceInstance = this
        isRunning = true
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationHandler.createNotificationChannel()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                startRecording()
            }
            ACTION_PAUSE_RECORDING -> {
                pauseRecording()
            }
            ACTION_RESUME_RECORDING -> {
                resumeRecording()
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
            }
        }
        
        // Return START_STICKY to ensure service restarts if killed
        return START_STICKY
    }
    
    /**
     * Start the foreground service with notification
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startForegroundWithNotification() {
        val notification = createRecordingNotification(RecordingState.Recording)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHandler.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NotificationHandler.NOTIFICATION_ID, notification)
        }
    }
    
    /**
     * Create notification for recording state
     */
    private fun createRecordingNotification(state: RecordingState): Notification {
        val builder = NotificationCompat.Builder(this, NotificationHandler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Screen Recording")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setSilent(true)
        
        when (state) {
            is RecordingState.Recording -> {
                builder.setContentText("Recording in progress...")
                    .addAction(
                        android.R.drawable.ic_media_pause,
                        "Pause",
                        createPausePendingIntent()
                    )
                    .addAction(
                        android.R.drawable.ic_delete,
                        "Stop",
                        createStopPendingIntent()
                    )
            }
            is RecordingState.Paused -> {
                builder.setContentText("Recording paused")
                    .addAction(
                        android.R.drawable.ic_media_play,
                        "Resume",
                        createResumePendingIntent()
                    )
                    .addAction(
                        android.R.drawable.ic_delete,
                        "Stop",
                        createStopPendingIntent()
                    )
            }
            else -> {
                builder.setContentText("Screen recorder active")
            }
        }
        
        return builder.build()
    }
    
    /**
     * Create pending intent for pause action
     */
    private fun createPausePendingIntent(): PendingIntent {
        val intent = Intent(this, ScreenRecorderForegroundService::class.java).apply {
            action = ACTION_PAUSE_RECORDING
        }
        return PendingIntent.getService(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Create pending intent for resume action
     */
    private fun createResumePendingIntent(): PendingIntent {
        val intent = Intent(this, ScreenRecorderForegroundService::class.java).apply {
            action = ACTION_RESUME_RECORDING
        }
        return PendingIntent.getService(
            this,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Create pending intent for stop action
     */
    private fun createStopPendingIntent(): PendingIntent {
        val intent = Intent(this, ScreenRecorderForegroundService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        return PendingIntent.getService(
            this,
            3,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Start recording
     */
    private fun startRecording() {
        currentState = RecordingState.Recording
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForegroundWithNotification()
        } else {
            val notification = createRecordingNotification(RecordingState.Recording)
            @Suppress("DEPRECATION")
            startForeground(NotificationHandler.NOTIFICATION_ID, notification)
        }
        
        // Notify service manager
        serviceManager?.onRecordingStarted()
    }
    
    /**
     * Pause recording
     */
    private fun pauseRecording() {
        currentState = RecordingState.Paused
        updateNotification()
        
        // Notify service manager
        serviceManager?.onRecordingPaused()
    }
    
    /**
     * Resume recording
     */
    private fun resumeRecording() {
        currentState = RecordingState.Recording
        updateNotification()
        
        // Notify service manager
        serviceManager?.onRecordingResumed()
    }
    
    /**
     * Stop recording
     */
    private fun stopRecording() {
        currentState = RecordingState.Stopped
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        
        // Notify service manager
        serviceManager?.onRecordingStopped()
    }
    
    /**
     * Update the foreground notification
     */
    private fun updateNotification() {
        val notification = createRecordingNotification(currentState)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NotificationHandler.NOTIFICATION_ID, notification)
    }
    
    /**
     * Set the service manager callback
     */
    fun setServiceManager(manager: ScreenRecorderServiceManager?) {
        serviceManager = manager
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceInstance = null
        isRunning = false
    }
}
