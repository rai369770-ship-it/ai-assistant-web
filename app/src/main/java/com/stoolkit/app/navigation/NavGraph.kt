package com.blindtechnexus.app.navigation

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Permission : Screen("permission")
    object Tools : Screen("tools")
    object Articles : Screen("articles")
    object Favorites : Screen("favorites")
    object More : Screen("more")
}

/**
 * Bottom navigation items
 */
sealed class BottomNavItem(
    val route: String,
    val title: String
) {
    object Tools : BottomNavItem("tools", "Tools")
    object Articles : BottomNavItem("articles", "Articles")
    object Favorites : BottomNavItem("favorites", "Favorites")
    object More : BottomNavItem("more", "More")
    
    companion object {
        val items = listOf(Tools, Articles, Favorites, More)
    }
}
