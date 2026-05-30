package com.taskpro.app.data

import android.content.Context
import com.taskpro.app.notification.ReminderScheduler
import com.taskpro.app.settings.SettingsRepository

/** Manual dependency container, created once in [com.taskpro.app.TaskProApp]. */
class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val scheduler = ReminderScheduler(context)

    val taskRepository: TaskRepository =
        TaskRepository(database.itemDao(), database.taskDao(), scheduler)

    val settingsRepository: SettingsRepository = SettingsRepository(context)
}
