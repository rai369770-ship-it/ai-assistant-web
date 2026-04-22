package com.stoolkit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stoolkit.app.navigation.BottomNavItem
import com.stoolkit.app.navigation.Screen
import com.stoolkit.app.ui.components.SToolkitBottomNavigation
import com.stoolkit.app.ui.screens.*
import com.stoolkit.app.ui.theme.SToolkitTheme

class MainActivity : ComponentActivity() {
    
    private var hasShownWelcome by mutableStateOf(false)
    private var hasGrantedPermissions by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            SToolkitTheme {
                MainAppContent(
                    hasShownWelcome = hasShownWelcome,
                    onWelcomeShown = { hasShownWelcome = true },
                    hasGrantedPermissions = hasGrantedPermissions,
                    onPermissionsGranted = { hasGrantedPermissions = true }
                )
            }
        }
    }
}

/**
 * Main app content with navigation
 */
@Composable
fun MainAppContent(
    hasShownWelcome: Boolean,
    onWelcomeShown: () -> Unit,
    hasGrantedPermissions: Boolean,
    onPermissionsGranted: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Determine start destination based on state
    val startDestination = if (!hasShownWelcome) {
        Screen.Welcome.route
    } else if (!hasGrantedPermissions) {
        Screen.Permission.route
    } else {
        Screen.Tools.route
    }
    
    // Show bottom nav only on main screens
    val showBottomNav = currentRoute in listOf(
        Screen.Tools.route,
        Screen.Articles.route,
        Screen.Favorites.route,
        Screen.More.route
    )
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav) {
                SToolkitBottomNavigation(
                    currentRoute = currentRoute ?: Screen.Tools.route,
                    onNavigateToTab = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Welcome screen
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onContinueClick = {
                        onWelcomeShown()
                        navController.navigate(Screen.Permission.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }
            
            // Permission screen
            composable(Screen.Permission.route) {
                PermissionScreen(
                    onPermissionsGranted = {
                        onPermissionsGranted()
                        navController.navigate(Screen.Tools.route) {
                            popUpTo(Screen.Permission.route) { inclusive = true }
                        }
                    }
                )
            }
            
            // Tools screen
            composable(Screen.Tools.route) {
                ToolsScreen(
                    onToolClick = { toolName ->
                        // Handle tool click - show toast or navigate to detail
                    }
                )
            }
            
            // Articles screen
            composable(Screen.Articles.route) {
                ArticlesScreen()
            }
            
            // Favorites screen
            composable(Screen.Favorites.route) {
                FavoritesScreen()
            }
            
            // More screen
            composable(Screen.More.route) {
                MoreScreen(
                    onMoreOptionsClick = {
                        // Handle more options click
                    }
                )
            }
        }
    }
}
