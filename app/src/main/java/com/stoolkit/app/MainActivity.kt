package com.blindtechnexus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.blindtechnexus.app.navigation.Screen
import com.blindtechnexus.app.ui.components.BlindTechNexusBottomNavigation
import com.blindtechnexus.app.ui.components.BlindTechNexusTopBar
import com.blindtechnexus.app.ui.screens.AboutScreen
import com.blindtechnexus.app.ui.screens.ArticlesScreen
import com.blindtechnexus.app.ui.screens.ContactUsScreen
import com.blindtechnexus.app.ui.screens.FavoritesScreen
import com.blindtechnexus.app.ui.screens.FeedbackScreen
import com.blindtechnexus.app.ui.screens.MoreScreen
import com.blindtechnexus.app.ui.screens.PermissionScreen
import com.blindtechnexus.app.ui.screens.Toast
import com.blindtechnexus.app.ui.screens.ToolsScreen
import com.blindtechnexus.app.ui.screens.WelcomeScreen
import com.blindtechnexus.app.ui.screens.openAccessibilitySettingsIntent
import com.blindtechnexus.app.ui.theme.SToolkitTheme

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
    val context = LocalContext.current
    var menuToast by remember { mutableStateOf<String?>(null) }

    val startDestination = when {
        !hasShownWelcome -> Screen.Welcome.route
        !hasGrantedPermissions -> Screen.Permission.route
        else -> Screen.Tools.route
    }

    val mainRoutes = setOf(
        Screen.Tools.route,
        Screen.Articles.route,
        Screen.Favorites.route,
        Screen.More.route
    )
    val showBars = currentRoute in mainRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (showBars) {
                BlindTechNexusTopBar(
                    centerTitle = when (currentRoute) {
                        Screen.Tools.route -> "Tools"
                        Screen.Articles.route -> "Articles"
                        Screen.Favorites.route -> "Favorites"
                        Screen.More.route -> "More"
                        else -> ""
                    },
                    onMenuClick = { selected ->
                        when (selected) {
                            "Accessibility settings" -> context.startActivity(openAccessibilitySettingsIntent())
                            "About" -> navController.navigate(Screen.About.route)
                            "Contact us" -> navController.navigate(Screen.Contact.route)
                            "Feedback" -> navController.navigate(Screen.Feedback.route)
                        }
                        menuToast = selected
                    }
                )
            }
        },
        bottomBar = {
            if (showBars) {
                BlindTechNexusBottomNavigation(
                    currentRoute = currentRoute ?: Screen.Tools.route,
                    onNavigateToTab = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
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

            composable(Screen.Tools.route) { ToolsScreen(onToolClick = {}) }
            composable(Screen.Articles.route) { ArticlesScreen() }
            composable(Screen.Favorites.route) { FavoritesScreen(onToolClick = {}) }
            composable(Screen.More.route) {
                MoreScreen(
                    onOpenFeedback = { navController.navigate(Screen.Feedback.route) },
                    onOpenContact = { navController.navigate(Screen.Contact.route) }
                )
            }
            composable(Screen.About.route) {
                AboutScreen(
                    onBackClick = { navController.popBackStack() },
                    onContactClick = { navController.navigate(Screen.Contact.route) },
                    onFeedbackClick = { navController.navigate(Screen.Feedback.route) }
                )
            }
            composable(Screen.Contact.route) {
                ContactUsScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Screen.Feedback.route) {
                FeedbackScreen(onBackClick = { navController.popBackStack() })
            }
        }

        menuToast?.let { message ->
            Toast(message = message, onDismiss = { menuToast = null })
        }
    }
}
