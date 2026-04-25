package com.stoolkit.app.features.screenrecorder.handler

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaProjection
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import com.blind_tech_nexus.app.features.screenrecorder.model.ScreenRecordingConfig

/**
 * Handler class for managing VirtualDisplay creation and management
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class VirtualDisplayHandler(
    private val mediaProjection: MediaProjection,
    private val config: ScreenRecordingConfig
) {
    private var virtualDisplay: VirtualDisplay? = null
    private var isCreated = false
    
    companion object {
        private const val DISPLAY_NAME = "ScreenRecorderDisplay"
        private const val VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR = 1
    }
    
    /**
     * Create a virtual display for screen capture
     */
    fun createVirtualDisplay(surface: Surface?): Boolean {
        return try {
            if (surface == null) {
                return false
            }
            
            // Calculate appropriate flags based on Android version
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR or
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_TRUSTED
            } else {
                @Suppress("DEPRECATION")
                VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR or
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
            }
            
            virtualDisplay = mediaProjection.createVirtualDisplay(
                DISPLAY_NAME,
                config.videoWidth,
                config.videoHeight,
                config.videoDpi,
                flags,
                surface,
                null,
                null
            )
            
            isCreated = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get the current virtual display
     */
    fun getVirtualDisplay(): VirtualDisplay? {
        return virtualDisplay
    }
    
    /**
     * Resize the virtual display (useful for orientation changes)
     */
    fun resize(width: Int, height: Int, dpi: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isCreated) {
            try {
                virtualDisplay?.resize(width, height, dpi)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Release the virtual display resources
     */
    fun release() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            isCreated = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Check if virtual display is active
     */
    fun isActive(): Boolean {
        return isCreated && virtualDisplay != null
    }
}
