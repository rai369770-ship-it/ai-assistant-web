package com.blindtechnexus.app.features.screenrecorder

import android.content.Intent
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun SuccessDialog(
    uri: Uri,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val playbackHandler = rememberPlaybackHandler(context, uri)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Screen recording succeeded") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Play your recorded video right here in app.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            playbackHandler.bind(this)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { playbackHandler.rewind() }) {
                        Text("Rewind 10s")
                    }

                    Button(onClick = { playbackHandler.togglePlayPause() }) {
                        Text(if (playbackHandler.isPlaying) "Pause" else "Play")
                    }

                    Button(onClick = { playbackHandler.fastForward() }) {
                        Text("Forward 10s")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "video/mp4"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                }
            ) {
                Text("Share")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (playbackHandler.isPlaying) playbackHandler.togglePlayPause()
                    onDismiss()
                }
            ) {
                Text("Close")
            }
        }
    )
}
