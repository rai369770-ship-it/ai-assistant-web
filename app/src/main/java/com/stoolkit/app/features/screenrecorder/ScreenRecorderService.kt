package com.blindtechnexus.app.features.screenrecorder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.blindtechnexus.app.MainActivity
import com.blindtechnexus.app.R
import java.io.File

class ScreenRecorderService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isPaused = false
    private var isMicrophoneCaptureEnabled = false

    companion object {
        const val ACTION_START = "com.blindtechnexus.app.screenrecorder.START"
        const val ACTION_PAUSE = "com.blindtechnexus.app.screenrecorder.PAUSE"
        const val ACTION_RESUME = "com.blindtechnexus.app.screenrecorder.RESUME"
        const val ACTION_STOP = "com.blindtechnexus.app.screenrecorder.STOP"

        const val CHANNEL_ID = "screen_recorder_channel"
        const val NOTIFICATION_ID = 5001

        var isRecording = false
            private set
    }

    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(intent)
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen recorder",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Screen recording controls"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startRecording(intent: Intent) {
        startAsForeground(isPaused = false, contentText = "Preparing screen recorder…")

        val resultCode = intent.getIntExtra(ScreenRecorderExtras.EXTRA_RESULT_CODE, -1)
        val resultData = intent.getParcelableExtra<Intent>(ScreenRecorderExtras.EXTRA_RESULT_DATA)
        val config = intent.getParcelableExtra<ScreenRecorderConfig>(ScreenRecorderExtras.EXTRA_CONFIG)
        isMicrophoneCaptureEnabled = config?.microphoneEnabled == true

        if (resultCode == -1 || resultData == null) {
            stopSelf()
            return
        }

        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
            mediaProjection?.registerCallback(
                object : MediaProjection.Callback() {
                    override fun onStop() {
                        stopRecording()
                    }
                },
                null
            )

            outputFile = ScreenRecorderFileManager.createOutputFile(this)
            val displayMetrics = getDisplayMetrics()
            mediaRecorder = createAndPrepareMediaRecorder(outputFile!!, displayMetrics)
            createVirtualDisplay(displayMetrics)
            mediaRecorder?.start()

            isRecording = true
            isPaused = false
            startAsForeground(isPaused = false)
        } catch (e: Exception) {
            e.printStackTrace()
            cleanupResources()
            stopSelf()
        }
    }

    private fun getDisplayMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            metrics.widthPixels = bounds.width()
            metrics.heightPixels = bounds.height()
            metrics.densityDpi = resources.displayMetrics.densityDpi
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
        }
        return metrics
    }

    private fun createAndPrepareMediaRecorder(
        file: File,
        displayMetrics: DisplayMetrics
    ): MediaRecorder {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            recorder.apply {
                val captureAudio = isMicrophoneCaptureEnabled
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                if (captureAudio) {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }

                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(file.absolutePath)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                if (captureAudio) {
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128_000)
                }
                setVideoEncodingBitRate(8_000_000)
                setVideoFrameRate(30)
                setVideoSize(displayMetrics.widthPixels, displayMetrics.heightPixels)

                prepare()
            }
        } catch (e: Exception) {
            recorder.release()
            throw e
        }

        return recorder
    }

    private fun createVirtualDisplay(displayMetrics: DisplayMetrics) {
        val surface = mediaRecorder?.surface ?: throw IllegalStateException("MediaRecorder surface is null")

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "screen-recording-display",
            displayMetrics.widthPixels,
            displayMetrics.heightPixels,
            displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            null,
            null
        )
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording && !isPaused) {
            mediaRecorder?.pause()
            isPaused = true
            startAsForeground(isPaused = true)
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording && isPaused) {
            mediaRecorder?.resume()
            isPaused = false
            startAsForeground(isPaused = false)
        }
    }

    private fun cleanupResources() {
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null

        virtualDisplay?.release()
        virtualDisplay = null

        mediaProjection?.stop()
        mediaProjection = null

        isRecording = false
        isPaused = false
        isMicrophoneCaptureEnabled = false
    }

    private fun stopRecording() {
        runCatching {
            if (isRecording) {
                mediaRecorder?.stop()
            }
        }
        cleanupResources()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startAsForeground(isPaused: Boolean, contentText: String = "Tap actions to control your recording") {
        val notification = buildRecordingNotification(isPaused = isPaused, contentText = contentText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && isMicrophoneCaptureEnabled) {
                serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildRecordingNotification(
        isPaused: Boolean,
        contentText: String
    ): android.app.Notification {
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseOrResumeAction = if (isPaused) {
            action("Resume recording", ACTION_RESUME, 101)
        } else {
            action("Pause recording", ACTION_PAUSE, 102)
        }
        val stopAction = action("Stop recording", ACTION_STOP, 103)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isPaused) "Screen recording paused" else "Recording in progress")
            .setContentText(contentText)
            .setOngoing(!isPaused)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(pauseOrResumeAction)
            .addAction(stopAction)
            .build()
    }

    private fun action(title: String, action: String, requestCode: Int): NotificationCompat.Action {
        val intent = Intent(this, ScreenRecorderService::class.java).setAction(action)
        val pendingIntent = PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(0, title, pendingIntent).build()
    }

    override fun onDestroy() {
        cleanupResources()
        super.onDestroy()
    }
}
