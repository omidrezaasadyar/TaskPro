package com.taskpro.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import com.taskpro.app.ui.screens.settings.RestoreRecovery
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.taskpro.app.ui.AppViewModelProvider
import com.taskpro.app.ui.components.ProvideBackgroundStyle
import com.taskpro.app.ui.navigation.NavGraph
import com.taskpro.app.ui.screens.settings.SettingsViewModel
import com.taskpro.app.ui.theme.TaskProTheme

class MainActivity : androidx.fragment.app.FragmentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            val settingsViewModel: SettingsViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val backgroundStyle by settingsViewModel.backgroundStyle.collectAsStateWithLifecycle()
            val backupState by settingsViewModel.backupState.collectAsStateWithLifecycle()

            TaskProTheme(themeMode = themeMode) {
                ProvideBackgroundStyle(backgroundStyle) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (backupState.ready) {
                            val navController = rememberNavController()
                            NavGraph(navController = navController)
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                RestoreRecovery(backupState.recoveryFailed, settingsViewModel::recover)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        (application as TaskProApp).container.vaultRepository.touch()
        return super.dispatchTouchEvent(event)
    }

    override fun onUserInteraction() {
        (application as TaskProApp).container.vaultRepository.touch()
        super.onUserInteraction()
    }

    override fun onStop() {
        if (!isChangingConfigurations) (application as TaskProApp).container.vaultRepository.setForeground(false)
        (application as TaskProApp).container.vaultClipboard.clearOwned()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        (application as TaskProApp).container.vaultRepository.setForeground(true)
    }
}
