package com.stoolkit.app.ui.screens

import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stoolkit.app.ui.theme.*
import android.content.Intent
import android.provider.Settings

/**
 * Permission screen composable for handling all files access and other permissions
 */
@Composable
fun PermissionScreen(
    onPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var hasAllFilesAccess by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }
        )
    }
    
    // Launcher for all files access permission
    val allFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasAllFilesAccess = Environment.isExternalStorageManager()
        }
        if (hasAllFilesAccess) {
            onPermissionsGranted()
        }
    }
    
    LaunchedEffect(hasAllFilesAccess) {
        if (hasAllFilesAccess) {
            onPermissionsGranted()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
            .semantics { contentDescription = "Permission screen" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = "Grant Access",
            style = MaterialTheme.typography.headlineLarge,
            color = OnBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = "Grant Access" }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Permission description
        Text(
            text = "SToolkit needs access to your files, camera, and microphone for full functionality. Please grant the necessary permissions in the settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceMedium,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5,
            modifier = Modifier.semantics { contentDescription = "Permission description text" }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Grant Access button
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+ - Show dialog for all files access
                    showDialog = true
                } else {
                    // Android 10 and below - Request storage permissions
                    // For older versions, we just proceed as permissions are granted at runtime
                    onPermissionsGranted()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics { contentDescription = "Grant Access button" },
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Grant Access",
                style = MaterialTheme.typography.titleMedium,
                color = OnPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status text
        if (hasAllFilesAccess) {
            Text(
                text = "Permission Granted",
                style = MaterialTheme.typography.bodyMedium,
                color = Primary,
                textAlign = TextAlign.Center
            )
        }
    }
    
    // Dialog for Android 11+ all files access
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = "All files access required",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnBackground
                )
            },
            text = {
                Text(
                    text = "All files permissions is required for files operations. Allow by granting in the setting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        allFilesAccessLauncher.launch(intent)
                    }
                ) {
                    Text(
                        text = "Grant Access",
                        color = Primary
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text(
                        text = "Cancel",
                        color = OnSurfaceDisabled
                    )
                }
            },
            containerColor = Surface,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceMedium
        )
    }
}
