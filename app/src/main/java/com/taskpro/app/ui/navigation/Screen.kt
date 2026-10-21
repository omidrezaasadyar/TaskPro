package com.taskpro.app.ui.navigation

/** Type-safe-ish route definitions for the app's navigation graph. */
sealed class Screen(val route: String) {
    data object Workspace : Screen("workspace")
    data object Dashboard : Screen("dashboard")
    data object Vault : Screen("vault")
    data object Home : Screen("home")
    data object Tasks : Screen("tasks")
    data object Search : Screen("search")
    data object Settings : Screen("settings")

    data object ItemDetail : Screen("item/{itemId}") {
        fun createRoute(itemId: Long) = "item/$itemId"
    }

    /** Per-item Completed/Snoozed list, reached from the item's summary boxes. */
    data object ItemStatusList : Screen("item/{itemId}/status/{status}") {
        fun createRoute(itemId: Long, status: String) = "item/$itemId/status/$status"
    }
}
