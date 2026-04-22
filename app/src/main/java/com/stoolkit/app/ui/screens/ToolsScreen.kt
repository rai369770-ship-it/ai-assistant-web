package com.stoolkit.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stoolkit.app.data.ToolsRepository
import com.stoolkit.app.data.model.CategoryGroup
import com.stoolkit.app.ui.theme.*

/**
 * Tools screen composable displaying all tools grouped by category
 */
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
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.semantics { contentDescription = "Tools list" }
        ) {
            items(categories, key = { it.name }) { category ->
                CategoryHeader(categoryName = category.name.displayName)
                items(category.tools) { tool ->
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
        }
        
        // Coming soon text at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text(
                text = "More tools coming soon",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDisabled,
                textAlign = TextAlign.Center,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
        
        // Toast notification
        if (showToast) {
            Toast(message = toastMessage, onDismiss = { showToast = false })
        }
    }
}

/**
 * Extension property to get display name for ToolCategory
 */
val com.stoolkit.app.data.model.ToolCategory.displayName: String
    get() = when (this) {
        com.stoolkit.app.data.model.ToolCategory.AI_TOOLS -> "AI Tools"
        com.stoolkit.app.data.model.ToolCategory.AUDIO_TOOLS -> "Audio Tools"
        com.stoolkit.app.data.model.ToolCategory.PRODUCTIVITY_TOOLS -> "Productivity Tools"
        com.stoolkit.app.data.model.ToolCategory.VIDEO_TOOLS -> "Video Tools"
        com.stoolkit.app.data.model.ToolCategory.IMAGE_TOOLS -> "Image Tools"
        com.stoolkit.app.data.model.ToolCategory.DEVICE_TOOLS -> "Device Tools"
    }

/**
 * Category header composable
 */
@Composable
fun CategoryHeader(
    categoryName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .semantics { contentDescription = "Category: $categoryName" }
    ) {
        Text(
            text = categoryName,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ),
            color = Primary,
            modifier = Modifier.semantics { contentDescription = "Category: $categoryName" }
        )
    }
}

/**
 * Tool item composable
 */
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
            .semantics { 
                contentDescription = "$name: $description. Opens tool details"
            },
        colors = CardDefaults.cardColors(
            containerColor = SurfaceTransparent
        ),
        border = CardDefaults.cardBorder(
            border = androidx.compose.foundation.BorderStroke(
                1.dp, 
                BorderTransparent
            )
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
            
            // Chevron icon
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = Secondary,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

/**
 * Toast notification composable
 */
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
            border = androidx.compose.foundation.BorderStroke(
                1.dp, 
                BorderTransparent
            ),
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
