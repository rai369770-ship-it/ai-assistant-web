package com.blind_tech_nexus.app.features.screenrecorder.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blind_tech_nexus.app.features.screenrecorder.handler.MediaPlayerHandler
import com.blind_tech_nexus.app.features.screenrecorder.handler.PermissionHandler
import com.blind_tech_nexus.app.features.screenrecorder.model.RecordingEvent
import com.blind_tech_nexus.app.features.screenrecorder.model.RecordingState
import com.blind_tech_nexus.app.features.screenrecorder.model.ScreenRecordingFile
import java.io.File

/**
 * Main Screen Recorder screen with all customization options and controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRecorderScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScreenRecorderViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    
    val uiState by viewModel.uiState.collectAsState()
    val recordingState by viewModel.recordingState.observeAsState(RecordingState.Idle)
    
    var showCompletionDialog by remember { mutableStateOf(false) }
    var showPlaybackDialog by remember { mutableStateOf(false) }
    var selectedRecordingFile by remember { mutableStateOf<ScreenRecordingFile?>(null) }
    
    // Media projection permission launcher
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            viewModel.handleMediaProjectionResult(result.resultCode, data)
        } else {
            viewModel.onPermissionDenied("Media projection permission denied")
        }
    }
    
    // Overlay permission launcher
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkOverlayPermission()
    }
    
    // Notification permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onNotificationPermissionResult(isGranted)
    }
    
    // Handle recording events
    LaunchedEffect(uiState.currentEvent) {
        uiState.currentEvent?.let { event ->
            when (event) {
                is RecordingEvent.RecordingStarted -> {
                    // Recording started
                }
                is RecordingEvent.RecordingStopped -> {
                    showCompletionDialog = true
                }
                is RecordingEvent.RecordingSaved -> {
                    // Recording saved
                }
                is RecordingEvent.RecordingError -> {
                    // Show error
                }
                else -> {}
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Screen Recorder") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                actions = {
                    Text(
                        text = "Record your screen with the following customization options",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Customizations section
            Text(
                text = "Customizations",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Microphone toggle
                    CustomizationOption(
                        icon = Icons.Default.Mic,
                        title = "Microphone",
                        description = "Turns on or off microphone while recording screen",
                        checked = uiState.config.enableMicrophone,
                        onCheckedChange = { viewModel.toggleMicrophone(it) }
                    )
                    
                    HorizontalDivider()
                    
                    // Device audio toggle
                    CustomizationOption(
                        icon = Icons.Default.VolumeUp,
                        title = "Device Audio",
                        description = "Turns on or off device audio while capturing screen",
                        checked = uiState.config.enableDeviceAudio,
                        onCheckedChange = { viewModel.toggleDeviceAudio(it) }
                    )
                    
                    HorizontalDivider()
                    
                    // Show touches toggle
                    CustomizationOption(
                        icon = Icons.Default.TouchApp,
                        title = "Show Touches",
                        description = "Turns on or off touches capture while recording",
                        checked = uiState.config.showTouches,
                        onCheckedChange = { viewModel.toggleShowTouches(it) }
                    )
                }
            }
            
            // Permission buttons
            AnimatedVisibility(
                visible = !uiState.hasOverlayPermission,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Button(
                    onClick = {
                        val intent = viewModel.permissionHandler.getOverlayPermissionIntent()
                        overlayPermissionLauncher.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Grant Display Over Other Apps Permission")
                }
            }
            
            AnimatedVisibility(
                visible = !uiState.hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Button(
                    onClick = {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Grant Notification Access")
                }
            }
            
            Spacer(Modifier.weight(1f))
            
            // Recording control button - only Start Recording is shown
            // Pause/Stop controls are in the floating overlay during recording
            if (recordingState == RecordingState.Idle || recordingState == RecordingState.Stopped) {
                Button(
                    onClick = {
                        val intent = viewModel.createMediaProjectionIntent()
                        mediaProjectionLauncher.launch(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Icon(Icons.Default.FiberManualRecord, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Recording", style = MaterialTheme.typography.titleMedium)
                }
                
                // Show status when recording
                if (recordingState == RecordingState.Stopped) {
                    Text(
                        text = "Recording completed. Check notification or dialog for options.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                // Show recording status message
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (recordingState == RecordingState.Recording) {
                                "Recording in progress... Controls are in floating overlay"
                            } else {
                                "Recording paused. Use floating overlay to resume or stop."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                // Info about floating controls
                Text(
                    text = "Use the floating overlay to pause/resume/stop recording. You can navigate to other apps while recording.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
    
    // Completion Dialog
    if (showCompletionDialog) {
        RecordingCompletionDialog(
            filePath = uiState.lastSavedPath,
            onDismiss = { showCompletionDialog = false },
            onPlay = { filePath ->
                selectedRecordingFile = ScreenRecordingFile(
                    filePath = filePath,
                    fileName = File(filePath).name,
                    createdAt = System.currentTimeMillis()
                )
                showPlaybackDialog = true
                showCompletionDialog = false
            },
            onShare = { filePath ->
                val shareIntent = viewModel.permissionHandler.getShareIntent(filePath)
                context.startActivity(Intent.createChooser(shareIntent, "Share recording"))
            },
            onDelete = { filePath ->
                viewModel.deleteRecording(filePath)
                showCompletionDialog = false
            }
        )
    }
    
    // Playback Dialog
    if (showPlaybackDialog && selectedRecordingFile != null) {
        PlaybackDialog(
            filePath = selectedRecordingFile!!.filePath,
            onDismiss = {
                showPlaybackDialog = false
                selectedRecordingFile = null
            }
        )
    }
}

/**
 * Customization option row component
 */
@Composable
private fun CustomizationOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Recording completion dialog
 */
@Composable
private fun RecordingCompletionDialog(
    filePath: String?,
    onDismiss: () -> Unit,
    onPlay: (String) -> Unit,
    onShare: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Screen Recording Completed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Screen recording is complete.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (filePath != null) {
                    Divider()
                    
                    Button(
                        onClick = { onPlay(filePath) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play Recording")
                    }
                    
                    Button(
                        onClick = { onShare(filePath) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share")
                    }
                    
                    Button(
                        onClick = { onDelete(filePath) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

/**
 * Playback dialog for viewing recorded videos
 */
@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun PlaybackDialog(
    filePath: String,
    onDismiss: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    DisposableEffect(filePath) {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            start()
            isPlaying = true
            
            setOnCompletionListener {
                isPlaying = false
            }
        }
        
        onDispose {
            mediaPlayer?.release()
        }
    }
    
    Dialog(onDismissRequest = {
        mediaPlayer?.stop()
        onDismiss()
    }) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Playing Recording",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = File(filePath).name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Divider()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (isPlaying) {
                                mediaPlayer?.pause()
                                isPlaying = false
                            } else {
                                mediaPlayer?.start()
                                isPlaying = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isPlaying) "Pause" else "Play")
                    }
                    
                    Button(
                        onClick = {
                            mediaPlayer?.stop()
                            mediaPlayer?.prepare()
                            mediaPlayer?.start()
                            isPlaying = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Stop Playing")
                    }
                }
                
                TextButton(
                    onClick = {
                        mediaPlayer?.stop()
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
