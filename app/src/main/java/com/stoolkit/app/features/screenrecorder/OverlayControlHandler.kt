package com.blindtechnexus.app.features.screenrecorder

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import com.blindtechnexus.app.R

class OverlayControlHandler(private val context: Context) {
    
    private val windowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private var overlayView: View? = null
    private var isShowing = false
    
    fun hasOverlayPermission(): Boolean = android.provider.Settings.canDrawOverlays(context)
    
    @RequiresApi(Build.VERSION_CODES.O)
    fun showRecordingControls(
        onPauseClick: () -> Unit,
        onResumeClick: () -> Unit,
        onStopClick: () -> Unit,
        onUpdatePauseButton: (isPaused: Boolean) -> Unit
    ) {
        if (isShowing) return
        if (!hasOverlayPermission()) return
        
        val layoutInflater = LayoutInflater.from(context)
        overlayView = layoutInflater.inflate(R.layout.overlay_recording_controls, null)
        
        val pauseButton = overlayView?.findViewById<ImageButton>(R.id.btnPauseResume)
        val stopButton = overlayView?.findViewById<ImageButton>(R.id.btnStop)
        
        var isPaused = false
        
        pauseButton?.setOnClickListener {
            if (isPaused) {
                onResumeClick()
                pauseButton.setImageResource(android.R.drawable.ic_media_pause)
                pauseButton.contentDescription = "Pause recording"
            } else {
                onPauseClick()
                pauseButton.setImageResource(android.R.drawable.ic_media_play)
                pauseButton.contentDescription = "Resume recording"
            }
            isPaused = !isPaused
        }
        
        stopButton?.setOnClickListener {
            // Hide overlay immediately when stop is clicked
            hideOverlay()
            // Then call stop action
            onStopClick()
        }
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 50
            y = 200
        }
        
        try {
            windowManager.addView(overlayView, params)
            isShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun hideOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // View might already be removed
            }
        }
        overlayView = null
        isShowing = false
    }
    
    fun updatePauseButton(isPaused: Boolean) {
        overlayView?.findViewById<ImageButton>(R.id.btnPauseResume)?.let { button ->
            if (isPaused) {
                button.setImageResource(android.R.drawable.ic_media_play)
                button.contentDescription = "Resume recording"
            } else {
                button.setImageResource(android.R.drawable.ic_media_pause)
                button.contentDescription = "Pause recording"
            }
        }
    }
}
