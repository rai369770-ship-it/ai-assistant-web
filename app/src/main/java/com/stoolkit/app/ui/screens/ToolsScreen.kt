package com.blindtechnexus.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blindtechnexus.app.data.ToolsRepository
import com.blindtechnexus.app.data.local.ToolStorageManager
import com.blindtechnexus.app.data.model.Tool
import com.blindtechnexus.app.ui.theme.BorderTransparent
import com.blindtechnexus.app.ui.theme.OnBackground
import com.blindtechnexus.app.ui.theme.OnSurface
import com.blindtechnexus.app.ui.theme.OnSurfaceDisabled
import com.blindtechnexus.app.ui.theme.Primary
import com.blindtechnexus.app.ui.theme.Secondary
import com.blindtechnexus.app.ui.theme.SurfaceTransparent

@Composable
fun ToolsScreen(
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember { ToolsRepository.getToolsByCategory() }
    val storageManager = remember { ToolStorageManager() }

    var favorites by remember { mutableStateOf(storageManager.readFavorites()) }
    var pinnedTools by remember { mutableStateOf(storageManager.readPinnedTools()) }

    var toastMessage by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }

    var selectedToolForActions by remember { mutableStateOf<Tool?>(null) }
    var selectedPinnedToolToUnpin by remember { mutableStateOf<Tool?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.semantics { contentDescription = "Tools list" }
        ) {
            if (pinnedTools.isNotEmpty()) {
                item("header-pinned") {
                    CategoryHeader(categoryName = "Pinned tools")
                }
                items(pinnedTools, key = { "pinned-${it.id}" }) { tool ->
                    ToolItem(
                        tool = tool,
                        onClick = {
                            toastMessage = "Coming soon"
                            showToast = true
                            onToolClick(tool.name)
                        },
                        onLongClick = {
                            selectedPinnedToolToUnpin = tool
                        }
                    )
                }
            }

            categories.forEach { category ->
                val filteredTools = category.tools.filter { tool ->
                    pinnedTools.none { pinnedTool -> pinnedTool.id == tool.id }
                }
                if (filteredTools.isNotEmpty()) {
                    item(key = "header-${category.name}") {
                        CategoryHeader(categoryName = category.name.displayName)
                    }
                    items(filteredTools, key = { it.id }) { tool ->
                        ToolItem(
                            tool = tool,
                            onClick = {
                                toastMessage = "Coming soon"
                                showToast = true
                                onToolClick(tool.name)
                            },
                            onLongClick = {
                                selectedToolForActions = tool
                            }
                        )
                    }
                }
            }

            item("footer") {
                Text(
                    text = "More tools coming soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDisabled,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }

        if (showToast) {
            Toast(message = toastMessage, onDismiss = { showToast = false })
        }
    }

    selectedToolForActions?.let { tool ->
        val isFavorite = favorites.any { it.id == tool.id }
        val isPinned = pinnedTools.any { it.id == tool.id }
        AlertDialog(
            onDismissRequest = { selectedToolForActions = null },
            title = { Text("Choose an action.") },
            text = {
                when {
                    isFavorite && isPinned -> Text("No action available.")
                    else -> Text("Select an action for ${tool.name}.")
                }
            },
            confirmButton = {
                if (!isFavorite) {
                    TextButton(
                        onClick = {
                            val updated = (favorites + tool).distinctBy { it.id }
                            storageManager.saveFavorites(updated)
                            favorites = updated
                            selectedToolForActions = null
                        }
                    ) { Text("Add to favorites") }
                }
            },
            dismissButton = {
                if (!isPinned) {
                    TextButton(
                        onClick = {
                            val updated = (pinnedTools + tool).distinctBy { it.id }
                            storageManager.savePinnedTools(updated)
                            pinnedTools = updated
                            selectedToolForActions = null
                        }
                    ) { Text("Pin") }
                } else {
                    TextButton(onClick = { selectedToolForActions = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    selectedPinnedToolToUnpin?.let { tool ->
        AlertDialog(
            onDismissRequest = { selectedPinnedToolToUnpin = null },
            title = { Text("Unpin ${tool.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updated = pinnedTools.filterNot { it.id == tool.id }
                        storageManager.savePinnedTools(updated)
                        pinnedTools = updated
                        selectedPinnedToolToUnpin = null
                    }
                ) {
                    Text("Unpin")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPinnedToolToUnpin = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

val com.blindtechnexus.app.data.model.ToolCategory.displayName: String
    get() = when (this) {
        com.blindtechnexus.app.data.model.ToolCategory.AI_TOOLS -> "AI Tools"
        com.blindtechnexus.app.data.model.ToolCategory.AUDIO_TOOLS -> "Audio Tools"
        com.blindtechnexus.app.data.model.ToolCategory.PRODUCTIVITY_TOOLS -> "Productivity Tools"
        com.blindtechnexus.app.data.model.ToolCategory.VIDEO_TOOLS -> "Video Tools"
        com.blindtechnexus.app.data.model.ToolCategory.IMAGE_TOOLS -> "Image Tools"
        com.blindtechnexus.app.data.model.ToolCategory.DEVICE_TOOLS -> "Device Tools"
    }

@Composable
fun CategoryHeader(
    categoryName: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = categoryName,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = Primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { heading() }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolItem(
    tool: Tool,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${tool.name}. ${tool.id}. ${tool.description}"
            },
        colors = CardDefaults.cardColors(containerColor = SurfaceTransparent),
        border = BorderStroke(1.dp, BorderTransparent),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tool.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = Secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDisabled,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.4
                )
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = Secondary,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun Toast(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        onDismiss()
    }

    Box(
        modifier = modifier
            .wrapContentSize()
            .semantics { contentDescription = message }
    ) {
        Surface(
            color = SurfaceTransparent,
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(1.dp, BorderTransparent),
            shadowElevation = 8.dp
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = OnBackground,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}
