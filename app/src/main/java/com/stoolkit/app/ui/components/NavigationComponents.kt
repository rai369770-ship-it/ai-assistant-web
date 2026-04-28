package com.blindtechnexus.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blindtechnexus.app.navigation.BottomNavItem
import com.blindtechnexus.app.ui.theme.OnBackground
import com.blindtechnexus.app.ui.theme.OnSurface
import com.blindtechnexus.app.ui.theme.OnSurfaceDisabled
import com.blindtechnexus.app.ui.theme.OnSurfaceMedium
import com.blindtechnexus.app.ui.theme.Primary
import com.blindtechnexus.app.ui.theme.SurfaceTransparent
import com.blindtechnexus.app.ui.theme.Transparent

@Composable
fun BlindTechNexusBottomNavigation(
    currentRoute: String,
    onNavigateToTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = SurfaceTransparent,
        contentColor = OnSurface,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        BottomNavItem.items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                icon = {
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
                    Text(text = item.title, style = MaterialTheme.typography.labelMedium)
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

@Composable
fun BlindTechNexusTopBar(
    centerTitle: String,
    onMenuClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 1.dp, color = SurfaceTransparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                Text(
                    text = "Blind Tech Nexus",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnBackground
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = centerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceMedium
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.semantics { contentDescription = "More options" }
                ) {
                    Text("⋮", style = MaterialTheme.typography.titleLarge, color = OnBackground)
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("Accessibility settings", "About", "Contact us", "Feedback").forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                expanded = false
                                onMenuClick(item)
                            }
                        )
                    }
                }
            }
        }
    }
}
