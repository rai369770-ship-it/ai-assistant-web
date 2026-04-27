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
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    val overlayPermissionChecker = remember(context) { OverlayControlHandler(context) }
    val projectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    var microphoneEnabled by remember { mutableStateOf(true) }
    var deviceAudioEnabled by remember { mutableStateOf(false) }
    var showTouchesEnabled by remember { mutableStateOf(false) }

    var hasOverlayPermission by remember { mutableStateOf(overlayPermissionChecker.hasOverlayPermission()) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Track recording state
    var isRecording by remember { mutableStateOf(ScreenRecorderService.isRecordingActive) }

    // Listen for recording state changes
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            isRecording = ScreenRecorderService.isRecordingActive
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRecordAudioPermission = granted
    }

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(context, ScreenRecorderService::class.java).apply {
                action = ScreenRecorderService.ACTION_START
                putExtra(ScreenRecorderExtras.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenRecorderExtras.EXTRA_RESULT_DATA, result.data)
                putExtra(ScreenRecorderExtras.EXTRA_START_DELAY_MS, 0L)
                putExtra(
                    ScreenRecorderExtras.EXTRA_CONFIG,
                    ScreenRecorderConfig(
                        microphoneEnabled = microphoneEnabled && hasRecordAudioPermission,
                        deviceAudioEnabled = deviceAudioEnabled,
                        showTouchesEnabled = showTouchesEnabled,
                        hasOverlayPermission = hasOverlayPermission
                    )
                )
            }

            try {
                ContextCompat.startForegroundService(context, serviceIntent)

                // Navigate back to tools screen immediately
                onRecordingInitiated()
            } catch (e: Exception) {
                Log.e("ScreenRecorderScreen", "Failed to start recording service", e)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = overlayPermissionChecker.hasOverlayPermission()
                hasNotificationPermission =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                hasRecordAudioPermission =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                // Update recording state on resume
                isRecording = ScreenRecorderService.isRecordingActive
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBackClick) {
            Text("Go back")
        }

        Text(
            text = "Screen recorder",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() }
        )

        Text(
            text = "Record your screen with the following customization options.",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Customizations",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )

        CheckboxRow(
            label = "Microphone",
            description = "Turns on or off microphone while recording screen.",
            checked = microphoneEnabled,
            onCheckedChange = { microphoneEnabled = it }
        )
        CheckboxRow(
            label = "Device audio",
            description = "Turns on or off device audio while capturing screen.",
            checked = deviceAudioEnabled,
            onCheckedChange = { deviceAudioEnabled = it }
        )
        CheckboxRow(
            label = "Show touches",
            description = "Turns on or off touches capture while recording.",
            checked = showTouchesEnabled,
            onCheckedChange = { showTouchesEnabled = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!hasOverlayPermission) {
            Button(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Grant display over other apps permission") }
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
                onClick = {
                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Grant microphone permission") }
        }

        if (isRecording) {
            // Show recording status instead of start button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Recording in progress...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Use floating controls or notification to stop recording",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Button(
                onClick = {
                    if (microphoneEnabled && !hasRecordAudioPermission) {
                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        return@Button
                    }
                    val captureIntent = projectionManager.createScreenCaptureIntent()
                    projectionLauncher.launch(captureIntent)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = hasNotificationPermission
            ) {
                Text("Start recording")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (hasOverlayPermission) {
                "Note: Floating controls will appear over other apps. You can also use notification action buttons to control recording."
            } else {
                "Note: Notification action buttons will be available to control your recording. Grant 'Display over other apps' permission for floating controls."
            },
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
