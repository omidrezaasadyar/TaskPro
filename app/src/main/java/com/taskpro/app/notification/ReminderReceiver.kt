package com.taskpro.app.notification

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.taskpro.app.R
import com.taskpro.app.TaskProApp

/**
 * Fires when a task reminder time is reached. Posts a high-importance
 * notification with sound AND triggers a full-screen [ReminderActivity] popup
 * so the alert is impossible to miss (it shows over the lock screen).
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_TITLE)
            ?: context.getString(R.string.reminder_default_title)

        // The full-screen popup that does the special UI + sound + vibration.
        val fullScreenIntent = Intent(context, ReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
            putExtra(ReminderScheduler.EXTRA_TASK_TITLE, title)
        }

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        val fullScreenPending = PendingIntent.getActivity(
            context, taskId.toInt(), fullScreenIntent, flags
        )

        val notification = NotificationCompat.Builder(context, TaskProApp.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_default_title))
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPending, true)
            .setContentIntent(fullScreenPending)
            .build()

        // POST_NOTIFICATIONS is required on Android 13+.
        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (canPost) {
            NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
        }

        // Also try to launch the popup directly. On modern Android the
        // full-screen intent above is the sanctioned path, but starting the
        // activity here helps when the app is in the foreground.
        runCatching { context.startActivity(fullScreenIntent) }
    }
}
