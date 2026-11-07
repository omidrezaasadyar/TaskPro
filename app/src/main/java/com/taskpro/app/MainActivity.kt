package com.taskpro.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.taskpro.app.ui.AppViewModelProvider
import com.taskpro.app.ui.components.ProvideBackgroundStyle
import com.taskpro.app.ui.navigation.NavGraph
import com.taskpro.app.ui.screens.LaunchIntro
import com.taskpro.app.ui.screens.IntroMode
import com.taskpro.app.ui.screens.introDayKey
import com.taskpro.app.ui.screens.chooseIntroMode
import com.taskpro.app.ui.screens.settings.SettingsViewModel
import com.taskpro.app.ui.screens.settings.RestoreRecovery
import com.taskpro.app.ui.theme.TaskProTheme
import java.util.TimeZone

class MainActivity : androidx.fragment.app.FragmentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val animateIntro = savedInstanceState == null &&
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        val today = introDayKey(System.currentTimeMillis(), TimeZone.getDefault())
        val preferences = getSharedPreferences("vaurivo_intro", MODE_PRIVATE)
        val introMode = chooseIntroMode(preferences.getInt("last_full_day", -1), today, animateIntro)
        if (introMode == IntroMode.FULL) preferences.edit().putInt("last_full_day", today).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splash ->
                // The Compose mark starts at the same position and color as this splash icon.
                splash.remove()
            }
        }
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            val settingsViewModel: SettingsViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val backgroundStyle by settingsViewModel.backgroundStyle.collectAsStateWithLifecycle()
            val backupState by settingsViewModel.backupState.collectAsStateWithLifecycle()
            var showIntro by remember { mutableStateOf(introMode != IntroMode.NONE) }
            LaunchedEffect(backupState.recoveryFailed) {
                if (backupState.recoveryFailed) showIntro = false
            }

            TaskProTheme(themeMode = themeMode) {
                ProvideBackgroundStyle(backgroundStyle) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Box(Modifier.fillMaxSize().then(if (showIntro && !backupState.recoveryFailed)
                                Modifier.clearAndSetSemantics {} else Modifier)) {
                                if (backupState.ready) {
                                    val navController = rememberNavController()
                                    NavGraph(navController = navController)
                                } else {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        RestoreRecovery(backupState.recoveryFailed, settingsViewModel::recover)
                                    }
                                }
                            }
                            if (showIntro && !backupState.recoveryFailed) {
                                LaunchIntro(onFinished = { showIntro = false }, mode = introMode)
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
