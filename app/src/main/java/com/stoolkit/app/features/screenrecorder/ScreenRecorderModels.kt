package com.blindtechnexus.app.features.screenrecorder

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScreenRecorderConfig(
    val microphoneEnabled: Boolean,
    val deviceAudioEnabled: Boolean,
    val showTouchesEnabled: Boolean
) : Parcelable

object ScreenRecorderActions {
    const val ACTION_START = "com.blindtechnexus.app.screenrecorder.START"
    const val ACTION_PAUSE = "com.blindtechnexus.app.screenrecorder.PAUSE"
    const val ACTION_RESUME = "com.blindtechnexus.app.screenrecorder.RESUME"
    const val ACTION_STOP = "com.blindtechnexus.app.screenrecorder.STOP"
}

object ScreenRecorderExtras {
    const val EXTRA_RESULT_CODE = "extra_result_code"
    const val EXTRA_RESULT_DATA = "extra_result_data"
    const val EXTRA_CONFIG = "extra_config"
}
