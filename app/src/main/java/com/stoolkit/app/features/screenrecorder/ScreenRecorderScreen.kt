package com.blindtechnexus.app.features.screenrecorder

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

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

    var lastRecordingFile by remember { mutableStateOf<Uri?>(null) }
    var showFinishedDialog by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val hasOverlayPermission = overlayHandler.hasOverlayPermission()
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
            val serviceIntent = Intent(context, ScreenRecorderService::class.java).apply {
                action = ScreenRecorderActions.ACTION_START
                putExtra(ScreenRecorderExtras.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenRecorderExtras.EXTRA_RESULT_DATA, result.data)
                putExtra(
                    ScreenRecorderExtras.EXTRA_CONFIG,
                    ScreenRecorderConfig(
                        microphoneEnabled = microphoneEnabled,
                        deviceAudioEnabled = deviceAudioEnabled,
                        showTouchesEnabled = showTouchesEnabled
                    )
                )
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TopBar(
            onBackClick = onBackClick,
            title = "Screen recorder",
            subtitle = "Record your screen with the following customization options."
        )

        Text(
            text = "Customizations",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )

        CheckboxRow(
            label = "Mikerophone",
            description = "Turns on or off mikerophone while recording screen.",
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
            ) { Text("Grant display over other app") }
        }

        if (!hasNotificationPermission) {
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Grant notification access") }
        }

        Button(
            onClick = {
                val captureIntent = projectionManager.createScreenCaptureIntent()
                projectionLauncher.launch(captureIntent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start recording")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val action = if (isPaused) ScreenRecorderActions.ACTION_RESUME else ScreenRecorderActions.ACTION_PAUSE
                    context.startService(Intent(context, ScreenRecorderService::class.java).setAction(action))
                    isPaused = !isPaused
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isPaused) "Resume recording" else "Pause recording")
            }
            Button(
                onClick = {
                    context.startService(
                        Intent(context, ScreenRecorderService::class.java).setAction(ScreenRecorderActions.ACTION_STOP)
                    )
                    lastRecordingFile = ScreenRecorderFileManager.latestRecordingFile()?.let { Uri.fromFile(it) }
                    showFinishedDialog = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop recording")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Recording path: /storage/emulated/0/blind-tech-nexus/screen recordings/",
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (showFinishedDialog) {
        AlertDialog(
            onDismissRequest = { showFinishedDialog = false },
            title = { Text("Screen recording completed") },
            text = { Text("Screen recording is complete.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = lastRecordingFile
                        if (uri != null) {
                            if (!isPlaying) {
                                mediaPlayer = MediaPlayer().apply {
                                    setDataSource(context, uri)
                                    prepare()
                                    start()
                                }
                                isPlaying = true
                            } else {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = null
                                isPlaying = false
                            }
                        }
                    }
                ) {
                    Text(if (isPlaying) "Stop playing" else if (isPaused) "Pause" else "Play recording")
                }
            },
            dismissButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(
                        onClick = {
                            lastRecordingFile?.let { uri ->
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "video/mp4"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share recording"))
                            }
                        }
                    ) { Text("Share") }

                    TextButton(
                        onClick = {
                            lastRecordingFile?.path?.let { path ->
                                kotlin.runCatching { java.io.File(path).delete() }
                            }
                            showFinishedDialog = false
                        }
                    ) { Text("Delete") }

                    TextButton(onClick = { showFinishedDialog = false }) { Text("Close") }
                }
            }
        )
    }
}

@Composable
private fun TopBar(onBackClick: () -> Unit, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        TextButton(onClick = onBackClick) {
            Text("Go back")
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }

        Text(
            text = subtitle,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun Context.openDisplayOverOtherAppsSettings() {
    val uri = Uri.parse("package:$packageName")
    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
    }
}
