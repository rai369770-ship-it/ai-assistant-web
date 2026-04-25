package com.stoolkit.app.features.screenrecorder.handler

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.*
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import com.blind_tech_nexus.app.features.screenrecorder.model.ScreenRecordingConfig
import java.io.File
import java.nio.ByteBuffer

/**
 * Handler class for managing MediaCodec functionality as an alternative to MediaRecorder
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class MediaCodecHandler(
    private val config: ScreenRecordingConfig,
    private val outputFile: File
) {
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var surface: Surface? = null
    private var isRunning = false
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var muxerStarted = false
    
    companion object {
        private const val MIME_TYPE = "video/avc"
        private const val TIMEOUT_US = 10000L
    }
    
    /**
     * Initialize MediaCodec encoder
     */
    fun initialize(): Boolean {
        return try {
            // Create and configure MediaCodec encoder
            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)
            
            val format = MediaFormat.createVideoFormat(MIME_TYPE, config.videoWidth, config.videoHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, config.videoBitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, config.videoFrameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            
            mediaCodec?.setCallback(object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                    // Input buffer available
                }
                
                override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                    handleOutputBuffer(codec, index, info)
                }
                
                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                    e.printStackTrace()
                }
                
                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                    // Format changed
                }
            }, null)
            
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            surface = mediaCodec?.createInputSurface()
            
            // Initialize MediaMuxer
            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Start encoding and recording
     */
    fun startRecording() {
        if (!isRunning) {
            try {
                mediaCodec?.start()
                isRunning = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Handle output buffers from MediaCodec
     */
    private fun handleOutputBuffer(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
        val outputBuffer = codec.getOutputBuffer(index)
        
        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
            // Skip codec config buffers
            codec.releaseOutputBuffer(index, false)
            return
        }
        
        if (info.size == 0) {
            codec.releaseOutputBuffer(index, false)
            return
        }
        
        // Start muxer if not already started
        if (!muxerStarted && videoTrackIndex == -1) {
            videoTrackIndex = mediaMuxer?.addTrack(codec.outputFormat) ?: -1
            if (audioTrackIndex != -1 || videoTrackIndex != -1) {
                mediaMuxer?.start()
                muxerStarted = true
            }
        }
        
        // Write buffer to muxer
        if (muxerStarted && videoTrackIndex != -1) {
            outputBuffer?.let { buffer ->
                buffer.position(info.offset)
                buffer.limit(info.offset + info.size)
                mediaMuxer?.writeSampleData(videoTrackIndex, buffer, info)
            }
        }
        
        codec.releaseOutputBuffer(index, false)
        
        // Check for end of stream
        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
            stopRecording()
        }
    }
    
    /**
     * Get the input surface for recording
     */
    fun getInputSurface(): Surface? {
        return surface
    }
    
    /**
     * Stop recording
     */
    fun stopRecording() {
        if (isRunning) {
            try {
                // Signal end of stream
                val inputBuffers = mediaCodec?.inputBuffers
                if (inputBuffers != null) {
                    val index = mediaCodec?.dequeueInputBuffer(TIMEOUT_US)
                    if (index != null && index >= 0) {
                        mediaCodec?.queueInputBuffer(
                            index,
                            0,
                            0,
                            System.currentTimeMillis(),
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                    }
                }
                
                // Wait for processing to complete
                Thread.sleep(500)
                
                isRunning = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Pause recording (not directly supported by MediaCodec, would need custom implementation)
     */
    fun pauseRecording() {
        // MediaCodec doesn't have built-in pause support
        // Would need to stop feeding frames or implement custom logic
    }
    
    /**
     * Resume recording
     */
    fun resumeRecording() {
        // Resume feeding frames to codec
    }
    
    /**
     * Release all resources
     */
    fun release() {
        try {
            stopRecording()
            
            mediaMuxer?.stop()
            mediaMuxer?.release()
            mediaMuxer = null
            
            surface?.release()
            surface = null
            
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
            
            virtualDisplay?.release()
            virtualDisplay = null
            
            muxerStarted = false
            videoTrackIndex = -1
            audioTrackIndex = -1
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
