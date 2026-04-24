package com.stoolkit.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stoolkit.app.navigation.BottomNavItem
import com.stoolkit.app.ui.theme.*

/**
 * Bottom navigation bar composable using Material3 NavigationBar
 */
@Composable
fun SToolkitBottomNavigation(
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = SurfaceTransparent,
        contentColor = OnSurface,
        tonalElevation = 0.dp,
        modifier = modifier.semantics { contentDescription = "Bottom navigation" }
    ) {
        BottomNavItem.items.forEach { item ->
            val selected = currentRoute == item.route
            
            NavigationBarItem(
                icon = {
                    // Simple text-based icons matching the original design
                    Text(
                        text = when (item) {
                            is BottomNavItem.Tools -> "🛠️"
                            is BottomNavItem.Articles -> "📄"
                            is BottomNavItem.Favorites -> "⭐"
                            is BottomNavItem.More -> "⋯"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                selected = selected,
                onClick = { onNavigateToTab(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Primary,
                    selectedTextColor = Primary,
                    unselectedIconColor = OnSurfaceDisabled,
                    unselectedTextColor = OnSurfaceDisabled,
                    indicatorColor = Transparent
                ),
                alwaysShowLabel = true
            )
        }
    }
}

/**
 * Top app bar composable with more options popup menu
 */
@Composable
fun SToolkitTopAppBar(
    title: String = "SToolkit",
    subtitle: String = "",
    onMoreClick: () -> Unit,
    onAccessibilitySettingsClick: () -> Unit = {},
    onAboutUsClick: () -> Unit = {},
    onContactUsClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPopupMenu by remember { mutableStateOf(false) }
    
    Surface(
        color = SurfaceTransparent,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - App name
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnBackground,
                    modifier = Modifier.semantics { contentDescription = "App name: $title" }
                )
            }
            
            // Center - Subtitle/Screen title
            Box(
                modifier = Modifier.weight(2f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurfaceMedium,
                    maxLines = 1
                )
            }
            
            // Right side - More button with popup menu
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = { showPopupMenu = true },
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = "More options. Opens more options menu" }
                ) {
                    Text(
                        text = "⋮",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnBackground
                    )
                }
                
                // Popup menu
                if (showPopupMenu) {
                    PopupMenu(
                        onDismiss = { showPopupMenu = false },
                        onAccessibilitySettingsClick = {
                            showPopupMenu = false
                            onAccessibilitySettingsClick()
                        },
                        onAboutUsClick = {
                            showPopupMenu = false
                            onAboutUsClick()
                        },
                        onContactUsClick = {
                            showPopupMenu = false
                            onContactUsClick()
                        },
                        onFeedbackClick = {
                            showPopupMenu = false
                            onFeedbackClick()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Popup menu for more options
 */
@Composable
fun PopupMenu(
    onDismiss: () -> Unit,
    onAccessibilitySettingsClick: () -> Unit,
    onAboutUsClick: () -> Unit,
    onContactUsClick: () -> Unit,
    onFeedbackClick: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Accessibility Settings") },
            onClick = onAccessibilitySettingsClick,
            leadingIcon = {
                Text("♿", style = MaterialTheme.typography.bodyLarge)
            }
        )
        DropdownMenuItem(
            text = { Text("About Us") },
            onClick = onAboutUsClick,
            leadingIcon = {
                Text("ℹ️", style = MaterialTheme.typography.bodyLarge)
            }
        )
        DropdownMenuItem(
            text = { Text("Contact Us") },
            onClick = onContactUsClick,
            leadingIcon = {
                Text("📞", style = MaterialTheme.typography.bodyLarge)
            }
        )
        DropdownMenuItem(
            text = { Text("Feedback") },
            onClick = onFeedbackClick,
            leadingIcon = {
                Text("💬", style = MaterialTheme.typography.bodyLarge)
            }
        )
    }
}
