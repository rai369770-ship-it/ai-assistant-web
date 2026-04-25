package com.blind_tech_nexus.app.features.screenrecorder.handler

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.blind_tech_nexus.app.features.screenrecorder.model.RecordingState

/**
 * Handler class for managing floating overlay controls during screen recording
 */
@RequiresApi(Build.VERSION_CODES.M)
class FloatOverlayHandler(private val context: Context) {
    
    private var windowManager: WindowManager? = null
    private var floatView: View? = null
    private var isShowing = false
    
    private var onPauseClick: (() -> Unit)? = null
    private var onStopClick: (() -> Unit)? = null
    private var onResumeClick: (() -> Unit)? = null
    
    companion object {
        private const val OVERLAY_WIDTH = 200
        private const val OVERLAY_HEIGHT = WindowManager.LayoutParams.WRAP_CONTENT
    }
    
    /**
     * Initialize the window manager
     */
    private fun initializeWindowManager() {
        if (windowManager == null) {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }
    }
    
    /**
     * Show floating overlay with recording controls
     */
    fun showFloatingOverlay(
        state: RecordingState = RecordingState.Recording,
        onPause: (() -> Unit)? = null,
        onStop: (() -> Unit)? = null,
        onResume: (() -> Unit)? = null
    ) {
        if (isShowing) {
            return
        }
        
        initializeWindowManager()
        
        try {
            // Create floating view programmatically
            floatView = createFloatingView(state)
            
            // Set click listeners
            onPauseClick = onPause
            onStopClick = onStop
            onResumeClick = onResume
            
            // Configure layout parameters for overlay
            val params = WindowManager.LayoutParams(
                OVERLAY_WIDTH,
                OVERLAY_HEIGHT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 16
                y = 100
            }
            
            windowManager?.addView(floatView, params)
            isShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Create the floating view with controls
     */
    private fun createFloatingView(state: RecordingState): View {
        // Create a simple layout programmatically
        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            setPadding(16, 16, 16, 16)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Status text
        val statusText = TextView(context).apply {
            text = when (state) {
                RecordingState.Recording -> "Recording..."
                RecordingState.Paused -> "Paused"
                else -> "Screen Recorder"
            }
            textSize = 14f
            setTextColor(android.graphics.Color.BLACK)
            gravity = android.view.Gravity.CENTER
        }
        container.addView(statusText)
        
        // Button container
        val buttonContainer = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        when (state) {
            RecordingState.Recording -> {
                // Pause button
                val pauseBtn = createImageButton(android.R.drawable.ic_media_pause, "Pause")
                pauseBtn.setOnClickListener { onPauseClick?.invoke() }
                buttonContainer.addView(pauseBtn)
                
                // Stop button
                val stopBtn = createImageButton(android.R.drawable.ic_menu_close_clear_cancel, "Stop")
                stopBtn.setOnClickListener { onStopClick?.invoke() }
                buttonContainer.addView(stopBtn)
            }
            RecordingState.Paused -> {
                // Resume button
                val resumeBtn = createImageButton(android.R.drawable.ic_media_play, "Resume")
                resumeBtn.setOnClickListener { onResumeClick?.invoke() }
                buttonContainer.addView(resumeBtn)
                
                // Stop button
                val stopBtn = createImageButton(android.R.drawable.ic_menu_close_clear_cancel, "Stop")
                stopBtn.setOnClickListener { onStopClick?.invoke() }
                buttonContainer.addView(stopBtn)
            }
            else -> {
                // No buttons for idle state
            }
        }
        
        container.addView(buttonContainer)
        
        return container
    }
    
    /**
     * Create an image button
     */
    private fun createImageButton(iconResId: Int, contentDesc: String): ImageButton {
        return ImageButton(context).apply {
            setImageResource(iconResId)
            setContentDescription(contentDesc)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                48.dpToPx(),
                48.dpToPx()
            ).apply {
                marginStart = 8.dpToPx()
            }
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
        }
    }
    
    /**
     * Update the floating overlay based on new state
     */
    fun updateOverlayState(state: RecordingState) {
        if (!isShowing || floatView == null) {
            return
        }
        
        try {
            // Remove old view
            windowManager?.removeView(floatView)
            
            // Create and add new view with updated state
            floatView = createFloatingView(state)
            
            val params = WindowManager.LayoutParams(
                OVERLAY_WIDTH,
                OVERLAY_HEIGHT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 16
                y = 100
            }
            
            windowManager?.addView(floatView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Hide the floating overlay
     */
    fun hideFloatingOverlay() {
        if (!isShowing || floatView == null) {
            return
        }
        
        try {
            windowManager?.removeView(floatView)
            floatView = null
            isShowing = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Check if overlay is currently showing
     */
    fun isOverlayShowing(): Boolean {
        return isShowing
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        hideFloatingOverlay()
        windowManager = null
        onPauseClick = null
        onStopClick = null
        onResumeClick = null
    }
    
    /**
     * Convert dp to pixels
     */
    private fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
