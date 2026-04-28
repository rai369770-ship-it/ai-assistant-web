package com.blindtechnexus.app.features.screenrecorder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay

@Composable
fun ScreenRecorderScreen(
    onBackClick: () -> Unit,
    onRecordingInitiated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val overlayHelper = remember(context) { OverlayControlHandler(context) }
    val projectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    var microphoneEnabled by remember { mutableStateOf(true) }
    var deviceAudioEnabled by remember { mutableStateOf(true) }
    var showTouchesEnabled by remember { mutableStateOf(false) }

    var hasOverlayPermission by remember { mutableStateOf(overlayHelper.hasOverlayPermission()) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var isRecording by remember { mutableStateOf(ScreenRecorderService.isRecordingActive) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotificationPermission = granted }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasRecordAudioPermission = granted }

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(context, ScreenRecorderService::class.java).apply {
                action = ScreenRecorderService.ACTION_START
                putExtra(ScreenRecorderExtras.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenRecorderExtras.EXTRA_RESULT_DATA, result.data)
                putExtra(
                    ScreenRecorderExtras.EXTRA_CONFIG,
                    ScreenRecorderConfig(
                        microphoneEnabled = microphoneEnabled && hasRecordAudioPermission,
                        deviceAudioEnabled = deviceAudioEnabled,
                        showTouchesEnabled = showTouchesEnabled,
                        hasOverlayPermission = hasOverlayPermission
                    )
                )
                putExtra(ScreenRecorderExtras.EXTRA_RECORD_MIC, microphoneEnabled && hasRecordAudioPermission)
                putExtra(ScreenRecorderExtras.EXTRA_RECORD_SYSTEM, deviceAudioEnabled)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
            onRecordingInitiated()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            isRecording = ScreenRecorderService.isRecordingActive
            delay(350)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = overlayHelper.hasOverlayPermission()
                hasNotificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                hasRecordAudioPermission =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBackClick) { Text("Go back") }
        Text("Screen recorder", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })

        CheckboxRow("Microphone", "Include microphone in recording.", microphoneEnabled) { microphoneEnabled = it }
        CheckboxRow("System audio", "Capture app/media playback (Android 10+).", deviceAudioEnabled) { deviceAudioEnabled = it }
        CheckboxRow("Show touches", "Show touch indicators while recording.", showTouchesEnabled) { showTouchesEnabled = it }

        if (!hasOverlayPermission) {
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Grant overlay permission") }
        }

        if (!hasNotificationPermission) {
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Grant notification permission") }
        }

        if (microphoneEnabled && !hasRecordAudioPermission) {
            Button(
                onClick = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Grant microphone permission") }
        }

        if (isRecording) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Recording in progress")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, ScreenRecorderService::class.java).setAction(ScreenRecorderService.ACTION_STOP)
                    )
                }) { Text("Stop recording") }
            }
        } else {
            Button(
                onClick = {
                    val required = (!microphoneEnabled || hasRecordAudioPermission) &&
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasNotificationPermission)
                    if (required) {
                        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                    }
                },
                enabled = (!microphoneEnabled || hasRecordAudioPermission) &&
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasNotificationPermission),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Start recording") }
        }

        Text(
            text = "Use notification and floating controls for pause/resume/stop.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun CheckboxRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
