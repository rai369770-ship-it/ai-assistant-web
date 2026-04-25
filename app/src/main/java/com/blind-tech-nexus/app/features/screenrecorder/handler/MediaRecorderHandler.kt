package com.blind_tech_nexus.app.features.screenrecorder.handler

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.annotation.RequiresApi
import com.blind_tech_nexus.app.features.screenrecorder.model.ScreenRecordingConfig
import java.io.File

/**
 * Handler class for managing MediaRecorder functionality
 */
class MediaRecorderHandler(
    private val config: ScreenRecordingConfig,
    private val outputFile: File
) {
    private var mediaRecorder: MediaRecorder? = null
    private var isPrepared = false
    
    /**
     * Initialize and configure the MediaRecorder
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun initialize(): Boolean {
        return try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(Looper.getMainLooper())
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            configureRecorder()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Configure MediaRecorder with appropriate settings
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun configureRecorder() {
        mediaRecorder?.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            
            // Configure audio source based on settings
            when {
                config.enableMicrophone && config.enableDeviceAudio -> {
                    setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                }
                config.enableMicrophone -> {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                config.enableDeviceAudio -> {
                    setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX)
                }
                else -> {
                    // No audio
                }
            }
            
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            
            if (config.enableMicrophone || config.enableDeviceAudio) {
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
            }
            
            setVideoSize(config.videoWidth, config.videoHeight)
            setVideoFrameRate(config.videoFrameRate)
            setVideoEncodingBitRate(config.videoBitRate)
            
            setOutputFile(outputFile.absolutePath)
            
            try {
                prepare()
                isPrepared = true
            } catch (e: Exception) {
                e.printStackTrace()
                isPrepared = false
            }
        }
    }
    
    /**
     * Get the input surface for recording
     */
    fun getInputSurface(): Surface? {
        return if (isPrepared) {
            mediaRecorder?.surface
        } else {
            null
        }
    }
    
    /**
     * Start recording
     */
    fun startRecording() {
        if (isPrepared) {
            try {
                mediaRecorder?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Stop recording
     */
    fun stopRecording() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            // Ignore illegal state exception when stopping
            e.printStackTrace()
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
            isPrepared = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Pause recording (API 24+)
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isPrepared) {
            try {
                mediaRecorder?.pause()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Resume recording (API 24+)
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isPrepared) {
            try {
                mediaRecorder?.resume()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
