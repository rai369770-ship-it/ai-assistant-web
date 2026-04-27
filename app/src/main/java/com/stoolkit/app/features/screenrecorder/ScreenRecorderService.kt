package com.blindtechnexus.app.features.screenrecorder

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.blindtechnexus.app.MainActivity
import com.blindtechnexus.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class ScreenRecorderService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var audioRecord: AudioRecord? = null
    private var audioThread: Thread? = null

    private var tempVideoFile: File? = null
    private var tempAudioFile: File? = null
    private var finalOutputFile: File? = null

    private var isRecording = false
    private var isPaused = false
    private var isMicrophoneCaptureEnabled = false
    private var isSystemAudioCaptureEnabled = false
    private var isStartRequested = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "ScreenRecorderService"

        const val ACTION_START = "com.blindtechnexus.app.screenrecorder.START"
        const val ACTION_PAUSE = "com.blindtechnexus.app.screenrecorder.PAUSE"
        const val ACTION_RESUME = "com.blindtechnexus.app.screenrecorder.RESUME"
        const val ACTION_STOP = "com.blindtechnexus.app.screenrecorder.STOP"

        const val CHANNEL_ID = "screen_recorder_channel"
        const val NOTIFICATION_ID = 5001

        var isRecordingActive = false
            private set
    }

    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(intent)
            ACTION_PAUSE -> togglePause()
            ACTION_RESUME -> togglePause()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Recorder",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Screen recording controls"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun startRecording(intent: Intent) {
        if (isRecording || isStartRequested) {
            Log.w(TAG, "Ignoring start request because recording is already active or starting")
            return
        }
        val resultCode = intent.getIntExtra(ScreenRecorderExtras.EXTRA_RESULT_CODE, Int.MIN_VALUE)
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(ScreenRecorderExtras.EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Intent>(ScreenRecorderExtras.EXTRA_RESULT_DATA)
        }
        val config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(ScreenRecorderExtras.EXTRA_CONFIG, ScreenRecorderConfig::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<ScreenRecorderConfig>(ScreenRecorderExtras.EXTRA_CONFIG)
        }
        val startDelayMs = intent.getLongExtra(ScreenRecorderExtras.EXTRA_START_DELAY_MS, 3_000L)

        isMicrophoneCaptureEnabled = config?.microphoneEnabled == true
        isSystemAudioCaptureEnabled = config?.deviceAudioEnabled == true

        if (resultCode != Activity.RESULT_OK || resultData == null) {
            Log.e(TAG, "MediaProjection permission denied or missing data")
            stopSelf()
            return
        }

        isStartRequested = true

        // Start foreground immediately with preparing status
        startAsForeground(isPaused = false, contentText = "Preparing screen recorder...")

        serviceScope.launch {
            try {
                // Show countdown notification
                if (startDelayMs > 0) {
                    val secondsLeft = (startDelayMs / 1000).toInt()
                    for (i in secondsLeft downTo 1) {
                        if (!isStartRequested) return@launch
                        startAsForeground(
                            isPaused = false,
                            contentText = "Recording starts in $i seconds..."
                        )
                        delay(1000)
                    }
                }

                // Navigate to home screen
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)

                // Initialize MediaProjection
                val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

                mediaProjection?.registerCallback(
                    object : MediaProjection.Callback() {
                        override fun onStop() {
                            Log.i(TAG, "MediaProjection stopped by system")
                            if (isRecording) {
                                stopRecording()
                            }
                        }
                    },
                    handler
                )

                // Create output files
                tempVideoFile = createTempVideoFile()
                if (isMicrophoneCaptureEnabled || isSystemAudioCaptureEnabled) {
                    tempAudioFile = createTempAudioFile()
                }

                // Get display metrics
                val displayMetrics = getDisplayMetrics()
                val safeWidth = max(2, displayMetrics.widthPixels - (displayMetrics.widthPixels % 2))
                val safeHeight = max(2, displayMetrics.heightPixels - (displayMetrics.heightPixels % 2))

                // Setup MediaRecorder
                setupMediaRecorder(safeWidth, safeHeight)

                // Setup system audio capture if enabled
                if (isSystemAudioCaptureEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setupSystemAudioCapture()
                }

                // Create VirtualDisplay
                setupVirtualDisplay(safeWidth, safeHeight, displayMetrics.densityDpi)

                // Start recording
                mediaRecorder?.start()
                isRecording = true
                isPaused = false
                isRecordingActive = true
                isStartRequested = false

                // Update notification to recording state
                startAsForeground(isPaused = false, contentText = "Recording in progress")

                Log.i(TAG, "Recording started at ${tempVideoFile?.absolutePath}")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                isStartRequested = false
                cleanupResources()
                stopSelf()
            }
        }
    }

    private fun createTempVideoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(cacheDir, "screen_record_$timestamp.mp4")
    }

    private fun createTempAudioFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(cacheDir, "screen_audio_$timestamp.pcm")
    }

    private fun createFinalOutputFile(): File {
        val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val appDir = File(baseDir, "BlindTechnexus/ScreenRecordings")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        return File(appDir, "ScreenRecording_$timestamp.mp4")
    }

    private fun getDisplayMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
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

    private fun setupMediaRecorder(width: Int, height: Int) {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)

            // Set audio source only for microphone (system audio is captured separately)
            if (isMicrophoneCaptureEnabled) {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(256000)
                setAudioSamplingRate(44100)
            }

            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(tempVideoFile?.absolutePath)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingBitRate(8_000_000)
            setVideoFrameRate(60)
            setVideoSize(width, height)

            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaRecorder error: what=$what, extra=$extra")
                stopRecording()
            }

            prepare()
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupSystemAudioCapture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        try {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT

            val audioFormatBuilder = AudioFormat.Builder()
                .setEncoding(audioFormat)
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)

            val playbackCaptureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .build()

            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufferSize, 16384)

            audioRecord = AudioRecord.Builder()
                .setAudioFormat(audioFormatBuilder.build())
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(playbackCaptureConfig)
                .build()

            // Start audio recording thread
            audioThread = Thread {
                val buffer = ShortArray(bufferSize / 2)
                var outputStream: FileOutputStream? = null

                try {
                    outputStream = FileOutputStream(tempAudioFile)
                    audioRecord?.startRecording()

                    while (isRecording && !Thread.interrupted()) {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                        if (read > 0) {
                            val byteBuffer = ByteBuffer.allocate(read * 2)
                            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                            for (i in 0 until read) {
                                byteBuffer.putShort(buffer[i])
                            }
                            outputStream.write(byteBuffer.array())
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Audio recording error", e)
                } finally {
                    try {
                        outputStream?.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error closing audio output stream", e)
                    }
                }
            }
            audioThread?.start()

            Log.i(TAG, "System audio capture initialized")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup system audio capture", e)
            isSystemAudioCaptureEnabled = false
        }
    }

    private fun setupVirtualDisplay(width: Int, height: Int, densityDpi: Int) {
        val surface = mediaRecorder?.surface
            ?: throw IllegalStateException("MediaRecorder surface is null")

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenRecorderDisplay",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
            surface,
            null,
            handler
        ) ?: throw IllegalStateException("Failed to create virtual display")

        Log.i(TAG, "VirtualDisplay created: ${width}x${height}@${densityDpi}dpi")
    }

    private fun togglePause() {
        if (!isRecording) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isPaused) {
                mediaRecorder?.resume()
                isPaused = false
                startAsForeground(isPaused = false, contentText = "Recording resumed")
            } else {
                mediaRecorder?.pause()
                isPaused = true
                startAsForeground(isPaused = true, contentText = "Recording paused")
            }
            Log.i(TAG, "Recording ${if (isPaused) "paused" else "resumed"}")
        }
    }

    private fun cleanupResources() {
        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing virtual display", e)
        }
        virtualDisplay = null

        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing media recorder", e)
        }
        mediaRecorder = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio record", e)
        }
        audioRecord = null

        audioThread?.interrupt()
        audioThread = null

        mediaProjection?.stop()
        mediaProjection = null

        isRecording = false
        isPaused = false
        isRecordingActive = false
        isStartRequested = false
    }

    private fun stopRecording() {
        if (!isRecording) {
            isStartRequested = false
            stopSelf()
            return
        }

        isRecording = false
        isRecordingActive = false

        serviceScope.launch {
            try {
                // Stop recorder
                runCatching {
                    mediaRecorder?.stop()
                }.onFailure { e ->
                    Log.w(TAG, "Error stopping media recorder", e)
                }

                // Stop audio recording
                audioRecord?.stop()
                audioThread?.join(1000)

                // Release resources
                cleanupResources()

                // Merge audio and video with FFmpeg
                mergeWithFFmpeg()

            } catch (e: Exception) {
                Log.e(TAG, "Error during stop recording", e)
                stopForeground(true)
                stopSelf()
            }
        }
    }

    private fun mergeWithFFmpeg() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                finalOutputFile = createFinalOutputFile()

                val command = buildFFmpegCommand()

                Log.i(TAG, "Executing FFmpeg command: $command")

                val session = FFmpegKit.execute(command)

                if (ReturnCode.isSuccess(session.returnCode)) {
                    Log.i(TAG, "FFmpeg merge successful: ${finalOutputFile?.absolutePath}")

                    // Delete temp files
                    tempVideoFile?.delete()
                    tempAudioFile?.delete()

                    // Scan file for gallery
                    scanFileForGallery()

                } else {
                    Log.e(TAG, "FFmpeg failed: ${session.failStackTrace}")

                    // If FFmpeg fails, use the original video file
                    if (tempVideoFile?.exists() == true) {
                        tempVideoFile?.copyTo(finalOutputFile!!, overwrite = true)
                        tempVideoFile?.delete()
                        tempAudioFile?.delete()
                        scanFileForGallery()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in FFmpeg merge", e)

                // Fallback: use original video
                if (tempVideoFile?.exists() == true) {
                    try {
                        finalOutputFile = createFinalOutputFile()
                        tempVideoFile?.copyTo(finalOutputFile!!, overwrite = true)
                        tempVideoFile?.delete()
                        tempAudioFile?.delete()
                        scanFileForGallery()
                    } catch (e2: Exception) {
                        Log.e(TAG, "Fallback copy also failed", e2)
                    }
                }
            } finally {
                stopForeground(true)
                stopSelf()
            }
        }
    }

    private fun buildFFmpegCommand(): String {
        val videoPath = tempVideoFile?.absolutePath ?: ""
        val audioPath = tempAudioFile?.absolutePath

        return if (!audioPath.isNullOrBlank() && (isMicrophoneCaptureEnabled || isSystemAudioCaptureEnabled)) {
            // Merge video with audio
            """
            -i "$videoPath" 
            -f s16le -ar 44100 -ac 1 -i "$audioPath" 
            -c:v copy 
            -c:a aac -b:a 256k 
            -shortest 
            -y "${finalOutputFile?.absolutePath}"
            """.trimIndent().replace("\n", " ")
        } else {
            // Just copy video
            """
            -i "$videoPath" 
            -c copy 
            -y "${finalOutputFile?.absolutePath}"
            """.trimIndent().replace("\n", " ")
        }
    }

    private fun scanFileForGallery() {
        val filePath = finalOutputFile?.absolutePath ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, finalOutputFile?.name)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/BlindTechnexus/ScreenRecordings")
            }

            val resolver = contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)

            uri?.let {
                resolver.openOutputStream(it)?.use { output ->
                    finalOutputFile?.inputStream()?.use { input ->
                        input.copyTo(output)
                    }
                }
                finalOutputFile?.delete()
            }
        } else {
            // For older versions, use MediaScannerConnection
            android.media.MediaScannerConnection.scanFile(
                this,
                arrayOf(filePath),
                arrayOf("video/mp4"),
                null
            )
        }

        Log.i(TAG, "File saved and scanned: $filePath")
    }

    private fun startAsForeground(isPaused: Boolean, contentText: String) {
        val notification = buildRecordingNotification(isPaused, contentText)

        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or
                if (isMicrophoneCaptureEnabled) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        } else {
            0
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildRecordingNotification(isPaused: Boolean, contentText: String): android.app.Notification {
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseOrResumeAction = if (isPaused) {
            buildAction("Resume", ACTION_RESUME, 101)
        } else {
            buildAction("Pause", ACTION_PAUSE, 102)
        }

        val stopAction = buildAction("Stop", ACTION_STOP, 103)

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

    private fun buildAction(title: String, action: String, requestCode: Int): NotificationCompat.Action {
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
        serviceScope.cancel()
        cleanupResources()
        super.onDestroy()
    }
}
