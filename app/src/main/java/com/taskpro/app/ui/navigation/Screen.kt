package com.taskpro.app.ui.navigation

/** Type-safe-ish route definitions for the app's navigation graph. */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Completed : Screen("completed")
    data object Snoozed : Screen("snoozed")
    data object Settings : Screen("settings")

    data object ItemDetail : Screen("item/{itemId}") {
        fun createRoute(itemId: Long) = "item/$itemId"
    }

    /** Per-item Completed/Snoozed list, reached from the item's summary boxes. */
    data object ItemStatusList : Screen("item/{itemId}/status/{status}") {
        fun createRoute(itemId: Long, status: String) = "item/$itemId/status/$status"
    }
}
