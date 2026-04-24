package com.blindtechnexus.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    var toastMessage by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.semantics { contentDescription = "Tools list" }
        ) {
            categories.forEach { category ->
                item(key = "header-${category.name}") {
                    CategoryHeader(categoryName = category.name.displayName)
                }
                items(category.tools, key = { it.name }) { tool ->
                    ToolItem(
                        name = tool.name,
                        description = tool.description,
                        onClick = {
                            toastMessage = "Coming soon"
                            showToast = true
                            onToolClick(tool.name)
                        }
                    )
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

@Composable
fun ToolItem(
    name: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "$name. $description"
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
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
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
