package com.stoolkit.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
    
    // Extract base route (without arguments)
    val baseRoute = currentRoute?.substringBefore("/")
    
    // Show bottom nav only on main screens
    val showBottomNav = baseRoute in listOf(
        Screen.Tools.route,
        Screen.Articles.route,
        Screen.Favorites.route,
        Screen.More.route
    )
    
    // Determine start destination based on state
    val startDestination = if (!hasShownWelcome) {
        Screen.Welcome.route
    } else if (!hasGrantedPermissions) {
        Screen.Permission.route
    } else {
        Screen.Tools.route
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav) {
                SToolkitBottomNavigation(
                    currentRoute = baseRoute ?: Screen.Tools.route,
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
                    navController = navController,
                    onToolClick = { toolName ->
                        // Handle tool click - show toast or navigate to detail
                    }
                )
            }
            
            // Articles screen
            composable(Screen.Articles.route) {
                ArticlesScreen(
                    navController = navController
                )
            }
            
            // Favorites screen
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    navController = navController
                )
            }
            
            // More screen
            composable(Screen.More.route) {
                MoreScreen(
                    navController = navController,
                    onAccessibilitySettingsClick = {
                        openAccessibilitySettings(navController.context)
                    },
                    onAboutUsClick = {
                        navController.navigate("about_us")
                    },
                    onContactUsClick = {
                        navController.navigate("contact_us")
                    },
                    onFeedbackClick = {
                        navController.navigate("feedback")
                    }
                )
            }
            
            // About Us screen
            composable("about_us") {
                AboutUsScreen(navController = navController)
            }
            
            // Contact Us screen
            composable("contact_us") {
                ContactUsScreen(navController = navController)
            }
            
            // Feedback screen
            composable("feedback") {
                FeedbackScreen(navController = navController)
            }
        }
    }
}

/**
 * Open accessibility settings
 */
fun openAccessibilitySettings(context: android.content.Context) {
    try {
        // First, try to open the specific accessibility service for this app
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
