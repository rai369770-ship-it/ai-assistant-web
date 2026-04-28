package com.blindtechnexus.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blindtechnexus.app.data.local.ToolStorageManager
import com.blindtechnexus.app.data.model.Tool
import com.blindtechnexus.app.ui.theme.OnSurfaceDisabled

/**
 * Articles screen composable - placeholder
 */
@Composable
fun ArticlesScreen(
    modifier: Modifier = Modifier
) {
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
    }
}

@Composable
fun FavoritesScreen(
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val storageManager = remember { ToolStorageManager() }
    var favoriteTools by remember { mutableStateOf(storageManager.readFavorites()) }
    var toolToRemove by remember { mutableStateOf<Tool?>(null) }
    var toastMessage by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item("favorites-heading") {
                Text(
                    text = "Favorite tools",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .semantics { heading() }
                )
            }

            if (favoriteTools.isEmpty()) {
                item("favorites-empty") {
                    Text(
                        text = "You don't have any favorite tools. Hold long press on a tool to add to favorites.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceDisabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(favoriteTools, key = { it.id }) { tool ->
                    ToolItem(
                        tool = tool,
                        onClick = {
                            toastMessage = "Coming soon"
                            showToast = true
                            onToolClick(tool.id)
                        },
                        onLongClick = { toolToRemove = tool }
                    )
                }
            }
        }

        if (showToast) {
            Toast(message = toastMessage, onDismiss = { showToast = false })
        }
    }

    toolToRemove?.let { tool ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { toolToRemove = null },
            title = { Text("Do you really want to remove ${tool.name} from favorites?") },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { toolToRemove = null }) {
                    Text("No")
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val updated = favoriteTools.filterNot { it.id == tool.id }
                        storageManager.saveFavorites(updated)
                        favoriteTools = updated
                        toolToRemove = null
                    }
                ) {
                    Text("Yes")
                }
            }
        )
    }
}

@Composable
fun MoreScreen(
    onOpenFeedback: () -> Unit,
    onOpenContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Visit our website for recent activities and tools on the web.")
            Button(
                onClick = { uriHandler.openUri("https://blindtechnexus.pages.dev") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Visit website")
            }
        }

        item {
            Text("Join us on official telegram channel for updates, announcements and more resources.")
            Button(
                onClick = { uriHandler.openUri("https://t.me/blindtechvisionary") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Join telegram")
            }
        }

        item {
            Text("Share your experience with the app with us to help improve the app.")
            Button(
                onClick = onOpenFeedback,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send feedback")
            }
        }

        item {
            Text("Explore more ways to contact us.")
            Button(
                onClick = onOpenContact,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View contacts")
            }
        }
    }
}
