package com.blindtechnexus.app.features.screenrecorder

import android.content.Context
import android.net.Uri
import android.widget.VideoView
import androidx.compose.runtime.*
import kotlinx.coroutines.delay

class PlaybackHandler(val context: Context, val uri: Uri) {
    private var videoView: VideoView? = null
    
    var isPlaying by mutableStateOf(false)
        private set
    var currentPosition by mutableStateOf(0)
        private set
    var duration by mutableStateOf(0)
        private set

    fun bind(view: VideoView) {
        videoView = view
        view.setVideoURI(uri)
        view.setOnPreparedListener { mp ->
            duration = mp.duration
            mp.setOnInfoListener { _, _, _ -> true }
            // Auto-start playback
            togglePlayPause()
        }
        view.setOnCompletionListener {
            isPlaying = false
            currentPosition = duration
        }
    }

    fun togglePlayPause() {
        videoView?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
            } else {
                it.start()
                isPlaying = true
            }
        }
    }

    fun rewind() {
        videoView?.let {
            val newPos = (it.currentPosition - 10000).coerceAtLeast(0)
            it.seekTo(newPos)
            currentPosition = newPos
        }
    }

    fun fastForward() {
        videoView?.let {
            val newPos = (it.currentPosition + 10000).coerceAtMost(duration)
            it.seekTo(newPos)
            currentPosition = newPos
        }
    }

    fun updateProgress() {
        videoView?.let {
            if (it.isPlaying) {
                currentPosition = it.currentPosition
            }
        }
    }
}

@Composable
fun rememberPlaybackHandler(context: Context, uri: Uri): PlaybackHandler {
    val handler = remember(context, uri) { PlaybackHandler(context, uri) }
    
    LaunchedEffect(handler.isPlaying) {
        while (handler.isPlaying) {
            handler.updateProgress()
            delay(500)
        }
    }
    
    return handler
}