package com.blind_tech_nexus.app.features.screenrecorder.handler

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.blind_tech_nexus.app.features.screenrecorder.R
import com.blind_tech_nexus.app.features.screenrecorder.model.RecordingState

/**
 * Handler class for managing notifications during screen recording
 */
class NotificationHandler(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "screen_recorder_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PAUSE = "com.blind_tech_nexus.action.PAUSE"
        const val ACTION_RESUME = "com.blind_tech_nexus.action.RESUME"
        const val ACTION_STOP = "com.blind_tech_nexus.action.STOP"
        
        private const val CHANNEL_NAME = "Screen Recorder"
        private const val CHANNEL_DESCRIPTION = "Notifications for screen recording"
    }
    
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    
    /**
     * Create notification channel (required for Android O and above)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = CHANNEL_DESCRIPTION
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        
        notificationManager.createNotificationChannel(channel)
    }
    
    /**
     * Show recording in progress notification
     */
    fun showRecordingNotification(
        state: RecordingState,
        pendingIntent: PendingIntent? = null
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Screen Recording")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setSilent(true)
        
        // Add action buttons based on current state
        when (state) {
            is RecordingState.Recording -> {
                builder.setContentText("Recording in progress...")
                    .addAction(
                        android.R.drawable.ic_media_pause,
                        "Pause",
                        createPausePendingIntent()
                    )
                    .addAction(
                        android.R.drawable.ic_media_pause,
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
                        android.R.drawable.ic_media_pause,
                        "Stop",
                        createStopPendingIntent()
                    )
            }
            else -> {
                builder.setContentText("Screen recorder ready")
            }
        }
        
        // Set content intent if provided
        pendingIntent?.let {
            builder.setContentIntent(it)
        }
        
        val notification = builder.build()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Show recording completed notification
     */
    fun showRecordingCompletedNotification(filePath: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("Screen Recording Completed")
            .setContentText("Tap to view your recording")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
        
        // Create intent to open the recording
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                android.net.Uri.fromFile(java.io.File(filePath)),
                "video/*"
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        builder.setContentIntent(pendingIntent)
        
        val notification = builder.build()
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
    
    /**
     * Dismiss the recording notification
     */
    fun dismissNotification() {
        try {
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Create pending intent for pause action
     */
    private fun createPausePendingIntent(): PendingIntent {
        val intent = Intent(ACTION_PAUSE)
        return PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Create pending intent for resume action
     */
    private fun createResumePendingIntent(): PendingIntent {
        val intent = Intent(ACTION_RESUME)
        return PendingIntent.getBroadcast(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Create pending intent for stop action
     */
    private fun createStopPendingIntent(): PendingIntent {
        val intent = Intent(ACTION_STOP)
        return PendingIntent.getBroadcast(
            context,
            3,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
