package com.blindtechnexus.app.features.screenrecorder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class ScreenRecorderService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var audioRecord: AudioRecord? = null
    private var audioThread: Thread? = null
    private val isAudioCaptureActive = AtomicBoolean(false)

    private var tempVideoFile: File? = null
    private var tempSystemAudioFile: File? = null

    private var isRecording = false
    private var isPaused = false
    private var isStopping = false

    private var recordMic = false
    private var recordSystemAudio = false
    private var showTouchesRequested = false
    private var shouldShowOverlay = false

    private var previousShowTouches: Int? = null

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val windowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private val overlayHandler by lazy { OverlayControlHandler(this) }

    companion object {
        private const val TAG = "ScreenRecorderService"

        const val ACTION_START = "com.blindtechnexus.app.screenrecorder.ACTION_START"
        const val ACTION_PAUSE_RESUME = "com.blindtechnexus.app.screenrecorder.ACTION_PAUSE_RESUME"
        const val ACTION_STOP = "com.blindtechnexus.app.screenrecorder.ACTION_STOP"

        private const val CHANNEL_ID = "screen_recorder_channel"
        private const val NOTIFICATION_ID = 4101

        private const val SAMPLE_RATE = 44_100
        private const val AUDIO_CHANNELS = 1
        private const val SHOW_TOUCHES_KEY = "show_touches"

        // State flows for Compose UI
        val isRecordingState = MutableStateFlow(false)
        val recordingCompletedEvent = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(intent)
            ACTION_PAUSE_RESUME -> togglePauseResume()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(intent: Intent) {
        if (isRecording || isStopping) return

        val resultCode = intent.getIntExtra(ScreenRecorderExtras.EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val resultData = intent.getParcelableCompat<Intent>(ScreenRecorderExtras.EXTRA_RESULT_DATA)
        val config = intent.getParcelableCompat<ScreenRecorderConfig>(ScreenRecorderExtras.EXTRA_CONFIG)

        recordMic = intent.getBooleanExtra(ScreenRecorderExtras.EXTRA_RECORD_MIC, config?.microphoneEnabled == true)
        recordSystemAudio = intent.getBooleanExtra(ScreenRecorderExtras.EXTRA_RECORD_SYSTEM, config?.deviceAudioEnabled == true)
        shouldShowOverlay = config?.hasOverlayPermission == true
        showTouchesRequested = config?.showTouchesEnabled == true

        if (resultCode != Activity.RESULT_OK || resultData == null) {
            stopSelf()
            return
        }

        startAsForeground()

        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
                ?: throw IllegalStateException("Unable to obtain MediaProjection")

            projectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.w(TAG, "MediaProjection revoked by system")
                    stopRecording()
                }
            }
            mediaProjection?.registerCallback(projectionCallback!!, handler)

            // Safe Resolution Calculation (Prevents MediaRecorder crashes)
            val metrics = currentDisplayMetrics()
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            val scale = if (screenWidth > 1080 || screenHeight > 1920) {
                minOf(1080f / screenWidth, 1920f / screenHeight)
            } else 1f

            val safeWidth = ((screenWidth * scale).toInt() / 16) * 16
            val safeHeight = ((screenHeight * scale).toInt() / 16) * 16
            val width = max(16, safeWidth)
            val height = max(16, safeHeight)

            tempVideoFile = File(cacheDir, "tmp_video_${System.currentTimeMillis()}.mp4")
            if (recordSystemAudio && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tempSystemAudioFile = File(cacheDir, "tmp_system_${System.currentTimeMillis()}.pcm")
            }

            applyShowTouches(showTouchesRequested)
            setupMediaRecorder(width, height)

            if (recordSystemAudio && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setupSystemAudioCapture()
            } else {
                recordSystemAudio = false
            }

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenRecorder",
                width,
                height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                handler
            ) ?: throw IllegalStateException("Unable to create VirtualDisplay")

            mediaRecorder?.start()
            isRecording = true
            isPaused = false
            isRecordingState.value = true

            updateNotification()
            maybeShowOverlay()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start recording", t)
            cleanupResources()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun setupMediaRecorder(width: Int, height: Int) {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()
        mediaRecorder?.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            if (recordMic) {
                setAudioSource(MediaRecorder.AudioSource.MIC)
            }
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(tempVideoFile?.absolutePath)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingBitRate(8_000_000)
            setVideoFrameRate(30) // 30fps is universally safer than 60fps
            setVideoSize(width, height)
            if (recordMic) {
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(256_000)
                setAudioSamplingRate(SAMPLE_RATE)
            }
            prepare()
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupSystemAudioCapture() {
        try {
            val projection = mediaProjection ?: return
            val channelMask = AudioFormat.CHANNEL_IN_MONO
            val encoding = AudioFormat.ENCODING_PCM_16BIT
            val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelMask, encoding)
            val bufferSize = max(minBuffer, SAMPLE_RATE)

            val format = AudioFormat.Builder()
                .setEncoding(encoding)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(channelMask)
                .build()

            val playback = AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .build()

            audioRecord = AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(playback)
                .build()

            val targetFile = tempSystemAudioFile ?: return
            isAudioCaptureActive.set(true)
            
            audioThread = Thread {
                FileOutputStream(targetFile).use { out ->
                    val shortBuffer = ShortArray(bufferSize / 2)
                    // Hoist allocation outside the loop to prevent GC churn/crashes!
                    val byteBuffer = ByteBuffer.allocate(shortBuffer.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                    
                    audioRecord?.startRecording()
                    while (isAudioCaptureActive.get()) {
                        val read = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: 0
                        if (read > 0) {
                            byteBuffer.clear()
                            byteBuffer.asShortBuffer().put(shortBuffer, 0, read)
                            out.write(byteBuffer.array(), 0, read * 2)
                        }
                    }
                }
            }.apply { start() }
        } catch (t: Throwable) {
            Log.w(TAG, "System audio capture unavailable", t)
            recordSystemAudio = false
        }
    }

    private fun togglePauseResume() {
        if (!isRecording || isStopping) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        runCatching {
            if (isPaused) mediaRecorder?.resume() else mediaRecorder?.pause()
            isPaused = !isPaused
            updateNotification()
            handler.post { overlayHandler.updatePauseState(isPaused) }
        }.onFailure {
            Log.w(TAG, "Pause/resume failed", it)
        }
    }

    private fun stopRecording() {
        if (isStopping) return
        isStopping = true

        if (!isRecording) {
            cleanupResources()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        isRecording = false
        isRecordingState.value = false

        scope.launch {
            try {
                runCatching { virtualDisplay?.release() }
                runCatching { mediaRecorder?.stop() }

                isAudioCaptureActive.set(false)
                runCatching { audioRecord?.stop() }
                runCatching { audioThread?.join(1_500) }

                val videoFile = tempVideoFile
                val systemAudioFile = tempSystemAudioFile

                cleanupResources(releaseTemp = false)

                if (videoFile == null || !videoFile.exists()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }

                val mergedOrVideo = mergeIfNeeded(videoFile, systemAudioFile)
                val galleryUri = saveToGallery(mergedOrVideo)
                
                if (galleryUri != null) {
                    sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, galleryUri))
                    // Notify UI to open the success dialog
                    recordingCompletedEvent.tryEmit(galleryUri)
                }

                if (mergedOrVideo.absolutePath != videoFile.absolutePath) {
                    videoFile.delete()
                }
                systemAudioFile?.delete()
                if (mergedOrVideo.exists()) mergedOrVideo.delete()
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun mergeIfNeeded(videoFile: File, systemAudioFile: File?): File {
        if (!recordSystemAudio || systemAudioFile == null || !systemAudioFile.exists()) {
            return videoFile
        }

        val mergedFile = File(cacheDir, "merged_${System.currentTimeMillis()}.mp4")
        val cmd = buildMergeCommand(videoFile, systemAudioFile, mergedFile)
        val session = FFmpegKit.execute(cmd)

        return if (ReturnCode.isSuccess(session.returnCode) && mergedFile.exists()) {
            mergedFile
        } else {
            mergedFile.delete()
            videoFile
        }
    }

    private fun buildMergeCommand(video: File, systemAudio: File, output: File): String {
        val quotedVideo = quote(video.absolutePath)
        val quotedSystem = quote(systemAudio.absolutePath)
        val quotedOut = quote(output.absolutePath)

        return if (recordMic) {
            "-i $quotedVideo -f s16le -ar $SAMPLE_RATE -ac $AUDIO_CHANNELS -i $quotedSystem " +
                "-filter_complex [0:a][1:a]amix=inputs=2:duration=first[a] -map 0:v -map [a] " +
                "-c:v copy -c:a aac -b:a 256k -y $quotedOut"
        } else {
            "-i $quotedVideo -f s16le -ar $SAMPLE_RATE -ac $AUDIO_CHANNELS -i $quotedSystem " +
                "-map 0:v -map 1:a -c:v copy -c:a aac -b:a 256k -y $quotedOut"
        }
    }

    private fun saveToGallery(source: File): Uri? {
        val name = "ScreenRecording_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
        val resolver = contentResolver

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/BlindTechCommunity")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            runCatching {
                resolver.openOutputStream(uri)?.use { output -> source.inputStream().use { it.copyTo(output) } }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }.getOrElse {
                resolver.delete(uri, null, null)
                return null
            }
            uri
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "BlindTechCommunity")
            if (!dir.exists()) dir.mkdirs()
            val out = File(dir, name)
            source.copyTo(out, overwrite = true)
            Uri.fromFile(out)
        }
    }

    private fun cleanupResources(releaseTemp: Boolean = true) {
        handler.post { overlayHandler.hide() }

        runCatching { virtualDisplay?.release() }
        virtualDisplay = null

        runCatching {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        }
        mediaRecorder = null

        isAudioCaptureActive.set(false)
        runCatching {
            audioRecord?.stop()
            audioRecord?.release()
        }
        audioRecord = null

        runCatching { audioThread?.interrupt() }
        audioThread = null

        projectionCallback?.let { cb -> runCatching { mediaProjection?.unregisterCallback(cb) } }
        projectionCallback = null
        runCatching { mediaProjection?.stop() }
        mediaProjection = null

        restoreShowTouches()

        isPaused = false
        isStopping = false
        isRecording = false
        isRecordingState.value = false

        if (releaseTemp) {
            tempVideoFile?.delete()
            tempSystemAudioFile?.delete()
        }
        tempVideoFile = null
        tempSystemAudioFile = null
    }

    private fun maybeShowOverlay() {
        if (!shouldShowOverlay || !overlayHandler.hasOverlayPermission()) return
        handler.post {
            overlayHandler.show(
                isPaused = isPaused,
                onTogglePause = { sendAction(ACTION_PAUSE_RESUME) },
                onStop = { sendAction(ACTION_STOP) }
            )
        }
    }

    private fun sendAction(action: String) {
        startService(Intent(this, ScreenRecorderService::class.java).setAction(action))
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+
            var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            if (recordMic) {
                types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(NOTIFICATION_ID, notification, types)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29
            // FOREGROUND_SERVICE_TYPE_MICROPHONE does not exist in API 29, so we safely omit it to prevent crashes
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else { // Below API 29
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            91,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val actionIntent = PendingIntent.getService(
            this,
            92,
            Intent(this, ScreenRecorderService::class.java).setAction(ACTION_PAUSE_RESUME),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            93,
            Intent(this, ScreenRecorderService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isPaused) getString(R.string.screen_recorder_paused_title) else getString(R.string.screen_recorder_recording_title)
        val pauseLabel = if (isPaused) getString(R.string.screen_recorder_resume) else getString(R.string.screen_recorder_pause)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(getString(R.string.screen_recorder_notification_text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(NotificationCompat.Action(0, pauseLabel, actionIntent))
            .addAction(NotificationCompat.Action(0, getString(R.string.screen_recorder_stop), stopIntent))
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Recorder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Controls active screen recording"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun currentDisplayMetrics(): DisplayMetrics {
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

    private fun applyShowTouches(enable: Boolean) {
        if (!Settings.System.canWrite(this)) return
        runCatching {
            if (previousShowTouches == null) {
                previousShowTouches = Settings.System.getInt(contentResolver, SHOW_TOUCHES_KEY, 0)
            }
            Settings.System.putInt(contentResolver, SHOW_TOUCHES_KEY, if (enable) 1 else 0)
        }
    }

    private fun restoreShowTouches() {
        val previous = previousShowTouches ?: return
        if (!Settings.System.canWrite(this)) return
        runCatching { Settings.System.putInt(contentResolver, SHOW_TOUCHES_KEY, previous) }
        previousShowTouches = null
    }

    override fun onDestroy() {
        cleanupResources()
        scope.cancel()
        super.onDestroy()
    }

    private fun quote(path: String): String = "\"${path.replace("\"", "\\\"")}\""

    private inline fun <reified T> Intent.getParcelableCompat(key: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key) as? T
        }
    }
}