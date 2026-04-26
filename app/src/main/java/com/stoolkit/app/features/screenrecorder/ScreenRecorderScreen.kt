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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ScreenRecorderScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val overlayHandler = remember { OverlayControlHandler(context) }
    val projectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    var microphoneEnabled by remember { mutableStateOf(true) }
    var deviceAudioEnabled by remember { mutableStateOf(false) }
    var showTouchesEnabled by remember { mutableStateOf(false) }

    var hasOverlayPermission by remember { mutableStateOf(overlayHandler.hasOverlayPermission()) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Start the foreground service with proper initialization
            // Overlay permission is optional - notification controls will be available as alternative
            val serviceIntent = Intent(context, ScreenRecorderService::class.java).apply {
                action = ScreenRecorderService.ACTION_START
                putExtra(ScreenRecorderExtras.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenRecorderExtras.EXTRA_RESULT_DATA, result.data)
                putExtra(
                    ScreenRecorderExtras.EXTRA_CONFIG,
                    ScreenRecorderConfig(
                        microphoneEnabled = microphoneEnabled,
                        deviceAudioEnabled = deviceAudioEnabled,
                        showTouchesEnabled = showTouchesEnabled,
                        hasOverlayPermission = hasOverlayPermission
                    )
                )
            }
            
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
                
                // Show overlay controls after a short delay if overlay permission is granted
                if (hasOverlayPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(800)
                        overlayHandler.showRecordingControls(
                            onPauseClick = {
                                context.startService(Intent(context, ScreenRecorderService::class.java).setAction(ScreenRecorderService.ACTION_PAUSE))
                            },
                            onResumeClick = {
                                context.startService(Intent(context, ScreenRecorderService::class.java).setAction(ScreenRecorderService.ACTION_RESUME))
                            },
                            onStopClick = {
                                context.startService(Intent(context, ScreenRecorderService::class.java).setAction(ScreenRecorderService.ACTION_STOP))
                                overlayHandler.hideOverlay()
                            },
                            onUpdatePauseButton = { isPaused ->
                                overlayHandler.updatePauseButton(isPaused)
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            overlayHandler.hideOverlay()
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
                    hasOverlayPermission = true
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

        Button(
            onClick = {
                val captureIntent = projectionManager.createScreenCaptureIntent()
                projectionLauncher.launch(captureIntent)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = hasNotificationPermission
        ) {
            Text("Start recording")
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
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
