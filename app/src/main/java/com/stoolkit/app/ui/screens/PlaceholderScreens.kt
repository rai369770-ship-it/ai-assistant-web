package com.stoolkit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stoolkit.app.ui.theme.*

/**
 * Articles screen composable - placeholder
 */
@Composable
fun ArticlesScreen(
    modifier: Modifier = Modifier
) {
    var toastMessage by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Articles coming soon",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfaceDisabled,
                textAlign = TextAlign.Center
            )
        }
        
        // Toast notification
        if (showToast) {
            Toast(message = toastMessage, onDismiss = { showToast = false })
        }
    }
}

/**
 * Favorites screen composable - placeholder
 */
@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier
) {
    var toastMessage by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Favorites coming soon",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfaceDisabled,
                textAlign = TextAlign.Center
            )
        }
        
        // Toast notification
        if (showToast) {
            Toast(message = toastMessage, onDismiss = { showToast = false })
        }
    }
}

/**
 * More screen composable - placeholder
 */
@Composable
fun MoreScreen(
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var toastMessage by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "More coming soon",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfaceDisabled,
                textAlign = TextAlign.Center
            )
        }
        
        // Toast notification
        if (showToast) {
            Toast(message = toastMessage, onDismiss = { showToast = false })
        }
    }
}
