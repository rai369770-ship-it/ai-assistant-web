package com.blindtechnexus.app.features.screenrecorder

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File

class ScreenRecorderService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var mediaCodec: MediaCodec? = null
    private var outputFile: File? = null
    private var isPaused = false

    private val notificationHandler by lazy { ScreenRecorderNotificationHandler(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ScreenRecorderActions.ACTION_START -> startRecording(intent)
            ScreenRecorderActions.ACTION_PAUSE -> pauseRecording()
            ScreenRecorderActions.ACTION_RESUME -> resumeRecording()
            ScreenRecorderActions.ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(intent: Intent) {
        notificationHandler.ensureChannel()
        startForeground(
            ScreenRecorderNotificationHandler.NOTIFICATION_ID,
            notificationHandler.buildRecordingNotification(isPaused = false)
        )

        val resultCode = intent.getIntExtra(ScreenRecorderExtras.EXTRA_RESULT_CODE, -1)
        val resultData = intent.getParcelableExtra<Intent>(ScreenRecorderExtras.EXTRA_RESULT_DATA)
        val config = intent.getParcelableExtra<ScreenRecorderConfig>(ScreenRecorderExtras.EXTRA_CONFIG)

        if (resultCode == -1 || resultData == null) {
            stopSelf()
            return
        }

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

        outputFile = ScreenRecorderFileManager.createOutputFile()
        prepareMediaRecorder(outputFile!!, config)
        createVirtualDisplay()
        mediaRecorder?.start()
    }

    private fun prepareMediaRecorder(file: File, config: ScreenRecorderConfig?) {
        @Suppress("DEPRECATION")
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }

        recorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            if (config?.microphoneEnabled == true || config?.deviceAudioEnabled == true) {
                setAudioSource(MediaRecorder.AudioSource.MIC)
            }
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(file.absolutePath)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncodingBitRate(8_000_000)
            setVideoFrameRate(30)
            setVideoSize(720, 1280)
            prepare()
        }

        mediaRecorder = recorder
    }

    private fun createVirtualDisplay() {
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "screen-recording-display",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface,
            null,
            null
        )
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.pause()
            isPaused = true
            startForeground(
                ScreenRecorderNotificationHandler.NOTIFICATION_ID,
                notificationHandler.buildRecordingNotification(isPaused = true)
            )
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.resume()
            isPaused = false
            startForeground(
                ScreenRecorderNotificationHandler.NOTIFICATION_ID,
                notificationHandler.buildRecordingNotification(isPaused = false)
            )
        }
    }

    private fun stopRecording() {
        runCatching { mediaRecorder?.stop() }
        mediaRecorder?.release()
        mediaRecorder = null

        virtualDisplay?.release()
        virtualDisplay = null

        mediaProjection?.stop()
        mediaProjection = null

        mediaCodec?.release()
        mediaCodec = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }
}
