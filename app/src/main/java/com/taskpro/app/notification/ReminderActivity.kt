package com.taskpro.app.notification

import android.app.KeyguardManager
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.NotificationManagerCompat
import com.taskpro.app.TaskProApp
import com.taskpro.app.ui.screens.reminder.ReminderPopupScreen
import com.taskpro.app.ui.theme.TaskProTheme
import com.taskpro.app.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A special, attention-grabbing full-screen reminder. Launched by
 * [ReminderReceiver] via a full-screen intent so it appears over the lock
 * screen and on top of whatever the user is doing, accompanied by sound and
 * vibration. The user can mark the task done, snooze it, or dismiss.
 */
class ReminderActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over the lock screen and turn the screen on.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)
                ?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_TITLE).orEmpty()

        startAlerting()

        setContent {
            // The popup uses its own vivid styling, so the system theme choice
            // is read but the screen mostly paints its own gradient.
            TaskProTheme(themeMode = ThemeMode.SYSTEM) {
                ReminderPopupScreen(
                    title = title,
                    onDone = {
                        markCompleted(taskId)
                        finishAndDismiss(taskId)
                    },
                    onSnooze = {
                        snooze(taskId, title)
                        finishAndDismiss(taskId)
                    },
                    onDismiss = { finishAndDismiss(taskId) }
                )
            }
        }
    }

    private fun startAlerting() {
        // Sound
        runCatching {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(this, uri).also { it.play() }
        }
        // Vibration pattern
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 400, 250, 400, 250, 600)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlerting() {
        runCatching { ringtone?.stop() }
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun markCompleted(taskId: Long) {
        if (taskId < 0) return
        val repo = (application as TaskProApp).container.taskRepository
        CoroutineScope(Dispatchers.IO).launch {
            repo.markCompletedById(taskId)
        }
    }

    private fun snooze(taskId: Long, title: String) {
        if (taskId < 0) return
        val repo = (application as TaskProApp).container.taskRepository
        CoroutineScope(Dispatchers.IO).launch {
            repo.snoozeByIdForMinutes(taskId, 10)
        }
    }

    private fun finishAndDismiss(taskId: Long) {
        stopAlerting()
        if (taskId >= 0) NotificationManagerCompat.from(this).cancel(taskId.toInt())
        finish()
    }

    override fun onDestroy() {
        stopAlerting()
        super.onDestroy()
    }
}
