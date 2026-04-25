package com.blindtechnexus.app.features.screenrecorder

import android.content.Context
import android.provider.Settings

class OverlayControlHandler(private val context: Context) {
    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)
}
