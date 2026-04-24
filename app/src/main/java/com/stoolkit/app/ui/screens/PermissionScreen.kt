package com.blindtechnexus.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.blindtechnexus.app.ui.theme.OnBackground
import com.blindtechnexus.app.ui.theme.OnPrimary
import com.blindtechnexus.app.ui.theme.OnSurfaceDisabled
import com.blindtechnexus.app.ui.theme.OnSurfaceMedium
import com.blindtechnexus.app.ui.theme.Primary

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(
    onPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasAllFilesAccess by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
        )
    }

    val runtimePermissions = remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    val runtimePermissionState = rememberMultiplePermissionsState(runtimePermissions)

    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasAllFilesAccess =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

        if (hasAllFilesAccess) {
            runtimePermissionState.launchMultiplePermissionRequest()
        }
    }

    val allRuntimeGranted = runtimePermissionState.permissions.all { it.status.isGranted }

    LaunchedEffect(hasAllFilesAccess, allRuntimeGranted) {
        if (hasAllFilesAccess && allRuntimeGranted) {
            onPermissionsGranted()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permissions",
            style = MaterialTheme.typography.headlineMedium,
            color = OnBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "To give you the best accessible experience, Blind Tech Nexus needs a few permissions: files access, storage read/write (where supported), microphone, camera, notifications, and background run support. We request all files first, then remaining permissions.",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceMedium,
            textAlign = TextAlign.Start,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "When all files access is granted, we will continue with audio, camera, and related permissions automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceDisabled,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasAllFilesAccess) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    allFilesAccessLauncher.launch(intent)
                } else {
                    runtimePermissionState.launchMultiplePermissionRequest()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics { contentDescription = "Grant permissions" },
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text(text = "Grant permissions", color = OnPrimary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary.copy(alpha = 0.85f))
        ) {
            Text(text = "Allow app to run in background", color = OnPrimary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        val statusText = when {
            hasAllFilesAccess && allRuntimeGranted -> "All required permissions granted"
            hasAllFilesAccess -> "All files access granted. Waiting for remaining permissions"
            else -> "Waiting for all files access"
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceDisabled,
            textAlign = TextAlign.Center
        )
    }
}
