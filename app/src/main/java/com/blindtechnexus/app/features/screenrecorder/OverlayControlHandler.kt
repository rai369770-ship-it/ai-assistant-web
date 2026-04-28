package com.blindtechnexus.app.features.screenrecorder

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.annotation.MainThread
import com.blindtechnexus.app.R

class OverlayControlHandler(private val context: Context) {

    private val windowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private var overlayView: View? = null

    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)

    @MainThread
    fun show(
        isPaused: Boolean,
        onTogglePause: () -> Unit,
        onStop: () -> Unit
    ) {
        if (overlayView != null || !hasOverlayPermission()) return

        val view = LayoutInflater.from(context).inflate(R.layout.overlay_recording_controls, null)
        val pauseButton = view.findViewById<ImageButton>(R.id.btnPauseResume)
        val stopButton = view.findViewById<ImageButton>(R.id.btnStop)

        pauseButton.setOnClickListener { onTogglePause() }
        stopButton.setOnClickListener { onStop() }
        setPauseStateInternal(pauseButton, isPaused)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 160
        }

        runCatching { windowManager.addView(view, params) }
        overlayView = view
    }

    @MainThread
    fun updatePauseState(isPaused: Boolean) {
        val pauseButton = overlayView?.findViewById<ImageButton>(R.id.btnPauseResume) ?: return
        setPauseStateInternal(pauseButton, isPaused)
    }

    @MainThread
    fun hide() {
        val current = overlayView ?: return
        runCatching { windowManager.removeView(current) }
        overlayView = null
    }

    private fun setPauseStateInternal(button: ImageButton, isPaused: Boolean) {
        if (isPaused) {
            button.setImageResource(android.R.drawable.ic_media_play)
            button.contentDescription = context.getString(R.string.screen_recorder_resume)
        } else {
            button.setImageResource(android.R.drawable.ic_media_pause)
            button.contentDescription = context.getString(R.string.screen_recorder_pause)
        }
    }
}
