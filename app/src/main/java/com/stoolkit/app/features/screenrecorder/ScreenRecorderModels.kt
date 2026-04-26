package com.blindtechnexus.app.features.screenrecorder

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScreenRecorderConfig(
    val microphoneEnabled: Boolean,
    val deviceAudioEnabled: Boolean,
    val showTouchesEnabled: Boolean
) : Parcelable

object ScreenRecorderExtras {
    const val EXTRA_RESULT_CODE = "extra_result_code"
    const val EXTRA_RESULT_DATA = "extra_result_data"
    const val EXTRA_CONFIG = "extra_config"
}
