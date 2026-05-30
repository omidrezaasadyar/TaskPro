package com.taskpro.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules and cancels exact alarms that deliver task reminders at the
 * chosen date/time. Each alarm is keyed by the task id so it can be replaced
 * or cancelled later.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(taskId: Long, title: String, triggerAtMillis: Long) {
        val pending = buildPendingIntent(taskId, title)

        // On Android 12+ exact alarms require permission; fall back gracefully.
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pending
            )
        } else {
            // Without exact-alarm permission, still deliver close to the time.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pending
            )
        }
    }

    fun cancel(taskId: Long) {
        alarmManager.cancel(buildPendingIntent(taskId, title = null))
    }

    private fun buildPendingIntent(taskId: Long, title: String?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            // A stable action keeps the PendingIntent "equal" for cancel().
            action = ACTION_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            title?.let { putExtra(EXTRA_TASK_TITLE, it) }
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, taskId.toInt(), intent, flags)
    }

    companion object {
        const val ACTION_REMINDER = "com.taskpro.app.ACTION_REMINDER"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
    }
}
