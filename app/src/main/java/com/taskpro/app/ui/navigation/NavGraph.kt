package com.taskpro.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.taskpro.app.ui.screens.completed.CompletedScreen
import com.taskpro.app.ui.screens.home.HomeScreen
import com.taskpro.app.ui.screens.item.ItemDetailScreen
import com.taskpro.app.ui.screens.settings.SettingsScreen
import com.taskpro.app.ui.screens.snoozed.SnoozedScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onItemClick = { id -> navController.navigate(Screen.ItemDetail.createRoute(id)) },
                onNavigate = { screen -> navController.navigate(screen.route) }
            )
        }

        composable(
            route = Screen.ItemDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) {
            ItemDetailScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Completed.route) {
            CompletedScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Snoozed.route) {
            SnoozedScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
