package com.taskpro.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import com.taskpro.app.TaskProApp
import com.taskpro.app.data.TaskStatus
import com.taskpro.app.ui.components.shared.BottomNavigationBar
import com.taskpro.app.ui.components.shared.WorkspaceTab
import com.taskpro.app.ui.screens.dashboard.DashboardRoute
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import com.taskpro.app.ui.screens.WorkspaceScreen
import com.taskpro.app.ui.screens.vault.VaultScreen
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.taskpro.app.ui.screens.home.HomeScreen
import com.taskpro.app.ui.screens.item.ItemDetailScreen
import com.taskpro.app.ui.screens.item.ItemStatusListScreen
import com.taskpro.app.ui.screens.search.SearchScreen
import com.taskpro.app.ui.screens.settings.SettingsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    val entry by navController.currentBackStackEntryAsState()
    val container = (LocalContext.current.applicationContext as TaskProApp).container
    val lockState = remember(container) { container.vaultRepository.state.map { it.document == null }.distinctUntilChanged() }
    val vaultLocked by lockState.collectAsStateWithLifecycle(initialValue = true)
    fun switch(screen: Screen) {
        val target = if (screen == Screen.Home) Screen.Tasks else screen
        navController.navigate(target.route) {
            popUpTo(Screen.Workspace.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
        if (entry != null && entry?.destination?.route != Screen.Workspace.route) {
            BottomNavigationBar(dashboardTab(entry?.destination?.route, navController.previousBackStackEntry?.destination?.route),
                onSelect = { tab -> switch(when (tab) {
                    WorkspaceTab.Home -> Screen.Dashboard
                    WorkspaceTab.Tasks -> Screen.Home
                    WorkspaceTab.Vault -> Screen.Vault
                })
                    if (tab == WorkspaceTab.Tasks) navController.popBackStack(Screen.Tasks.route, false)
                }, vaultLocked = vaultLocked)
        }
    }) { padding ->
    NavHost(navController = navController, startDestination = Screen.Workspace.route,
        modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
        composable(Screen.Workspace.route) { WorkspaceScreen(vaultLocked = vaultLocked, onSelect = ::switch) }
        composable(Screen.Dashboard.route) {
            DashboardRoute(vaultLocked, onTasks = { switch(Screen.Home); navController.popBackStack(Screen.Tasks.route, false) }, onVault = { switch(Screen.Vault) },
                onSettings = { navController.navigate(Screen.Settings.route) }, onChooser = { switch(Screen.Workspace) },
                onTask = { row -> navController.navigate(if (row.task.status == TaskStatus.PENDING)
                    Screen.ItemDetail.createRoute(row.task.itemId)
                    else Screen.ItemStatusList.createRoute(row.task.itemId, row.task.status.name)) })
        }
        composable(Screen.Vault.route) {
            VaultScreen(container.vaultRepository, container.vaultClipboard,
                onWorkspace = { switch(Screen.Workspace) }, onSettings = { navController.navigate(Screen.Settings.route) },
                deviceUnlock = container.vaultDeviceUnlock)
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onItemClick = { id -> navController.navigate(Screen.ItemDetail.createRoute(id)) },
                onNavigate = { screen ->
                    if (screen == Screen.Tasks) {
                        if (!navController.popBackStack(Screen.Tasks.route, false)) navController.navigate(Screen.Tasks.route) { launchSingleTop = true }
                    } else navController.navigate(screen.route)
                }
            )
        }

        composable(Screen.Tasks.route) {
            com.taskpro.app.ui.screens.tasks.TimelineRoute(
                onGroups = { navController.navigate(Screen.Home.route) { launchSingleTop = true } },
                onSearch = { navController.navigate(Screen.Search.route) },
                onOpenStatus = { id, status -> navController.navigate(Screen.ItemStatusList.createRoute(id, status)) })
        }

        composable(
            route = Screen.ItemDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) {
            ItemDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenStatusList = { itemId, status ->
                    navController.navigate(Screen.ItemStatusList.createRoute(itemId, status))
                }
            )
        }

        composable(
            route = Screen.ItemStatusList.route,
            arguments = listOf(
                navArgument("itemId") { type = NavType.LongType },
                navArgument("status") { type = NavType.StringType }
            )
        ) {
            ItemStatusListScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onResultClick = { itemId ->
                    navController.navigate(Screen.ItemDetail.createRoute(itemId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
    }
}

internal fun dashboardTab(route: String?, previousRoute: String? = null): WorkspaceTab = when (route) {
    Screen.Vault.route -> WorkspaceTab.Vault
    Screen.Tasks.route, Screen.Home.route, Screen.ItemDetail.route, Screen.ItemStatusList.route, Screen.Search.route -> WorkspaceTab.Tasks
    Screen.Settings.route -> if (previousRoute == Screen.Settings.route) WorkspaceTab.Home else dashboardTab(previousRoute)
    else -> WorkspaceTab.Home
}
