package com.blindtechnexus.app.features.screenrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.blindtechnexus.app.MainActivity
import com.blindtechnexus.app.R

class ScreenRecorderNotificationHandler(
    private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen recorder",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Screen recording controls"
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun buildRecordingNotification(isPaused: Boolean): Notification {
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            100,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseOrResumeAction = if (isPaused) {
            action("Resume recording", ScreenRecorderActions.ACTION_RESUME, 101)
        } else {
            action("Pause recording", ScreenRecorderActions.ACTION_PAUSE, 102)
        }
        val stopAction = action("Stop recording", ScreenRecorderActions.ACTION_STOP, 103)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isPaused) "Screen recording paused" else "Recording in progress")
            .setContentText("Tap actions to control your recording")
            .setOngoing(!isPaused)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(pauseOrResumeAction)
            .addAction(stopAction)
            .build()
    }

    private fun action(title: String, action: String, requestCode: Int): NotificationCompat.Action {
        val intent = Intent(context, ScreenRecorderService::class.java).setAction(action)
        val pendingIntent = PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(0, title, pendingIntent).build()
    }

    companion object {
        const val CHANNEL_ID = "screen_recorder_channel"
        const val NOTIFICATION_ID = 5001
    }
}
