package com.stoolkit.app.features.screenrecorder.handler

import android.content.Context
import android.media.MediaPlayer
import android.view.Surface
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blind_tech_nexus.app.features.screenrecorder.model.RecordingState

/**
 * Handler class for managing media playback of recorded videos
 */
class MediaPlayerHandler(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    private val _playbackState = MutableLiveData<PlaybackState>()
    private val _currentPosition = MutableLiveData<Int>()
    
    val playbackState: LiveData<PlaybackState> = _playbackState
    val currentPosition: LiveData<Int> = _currentPosition
    
    private var currentFilePath: String? = null
    
    /**
     * Sealed class representing playback states
     */
    sealed class PlaybackState {
        object Idle : PlaybackState()
        object Playing : PlaybackState()
        object Paused : PlaybackState()
        object Stopped : PlaybackState()
        data class Error(val message: String) : PlaybackState()
    }
    
    /**
     * Initialize player with video file
     */
    fun initialize(filePath: String): Boolean {
        return try {
            release()
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnPreparedListener {
                    _playbackState.value = PlaybackState.Playing
                    start()
                    startProgressUpdates()
                }
                setOnCompletionListener {
                    _playbackState.value = PlaybackState.Stopped
                    _currentPosition.value = 0
                    stopProgressUpdates()
                }
                setOnErrorListener { _, what, extra ->
                    _playbackState.value = PlaybackState.Error("Error $what, $extra")
                    true
                }
                prepareAsync()
            }
            
            currentFilePath = filePath
            _playbackState.value = PlaybackState.Idle
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _playbackState.value = PlaybackState.Error(e.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * Set surface for video rendering
     */
    fun setSurface(surface: Surface?) {
        mediaPlayer?.setDisplay(surface?.let { android.view.SurfaceHolder { it } })
    }
    
    /**
     * Start or resume playback
     */
    fun play() {
        if (_playbackState.value is PlaybackState.Paused || 
            _playbackState.value is PlaybackState.Idle) {
            mediaPlayer?.start()
            _playbackState.value = PlaybackState.Playing
            startProgressUpdates()
        }
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        if (_playbackState.value is PlaybackState.Playing) {
            mediaPlayer?.pause()
            _playbackState.value = PlaybackState.Paused
            stopProgressUpdates()
        }
    }
    
    /**
     * Stop playback
     */
    fun stop() {
        if (mediaPlayer != null && 
            (_playbackState.value is PlaybackState.Playing || 
             _playbackState.value is PlaybackState.Paused)) {
            mediaPlayer?.seekTo(0)
            mediaPlayer?.pause()
            _playbackState.value = PlaybackState.Stopped
            _currentPosition.value = 0
            stopProgressUpdates()
        }
    }
    
    /**
     * Seek to specific position
     */
    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        _currentPosition.value = position
    }
    
    /**
     * Get video duration
     */
    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }
    
    /**
     * Get current position
     */
    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }
    
    /**
     * Check if playing
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }
    
    /**
     * Start progress updates
     */
    private fun startProgressUpdates() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (_playbackState.value is PlaybackState.Playing) {
                    _currentPosition.value = getCurrentPosition()
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(runnable)
    }
    
    /**
     * Stop progress updates
     */
    private fun stopProgressUpdates() {
        // Handler will stop automatically when state changes
    }
    
    /**
     * Release resources
     */
    fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            currentFilePath = null
            _playbackState.value = PlaybackState.Idle
            _currentPosition.value = 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        when (_playbackState.value) {
            is PlaybackState.Playing -> pause()
            is PlaybackState.Paused, is PlaybackState.Idle, is PlaybackState.Stopped -> play()
            else -> {}
        }
    }
}
