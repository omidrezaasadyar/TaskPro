package com.taskpro.app.util

import android.content.Context
import android.content.Intent
import com.taskpro.app.R
import com.taskpro.app.data.Task
import com.taskpro.app.data.TaskStatus

/** Builds and shares a plain-text copy of an item's task list (e.g. to WhatsApp). */
object ShareUtil {

    fun buildText(context: Context, itemName: String, tasks: List<Task>): String {
        val sb = StringBuilder()
        sb.append("📋 ").append(itemName).append('\n')
        sb.append("──────────────\n")

        fun section(titleRes: Int, emoji: String, status: TaskStatus) {
            val group = tasks.filter { it.status == status }
            if (group.isEmpty()) return
            sb.append('\n').append(emoji).append(' ')
                .append(context.getString(titleRes)).append('\n')
            group.forEach { task ->
                sb.append("• ").append(task.title)
                task.dueAt?.let { sb.append("  (").append(DateFormat.formatDateTime(it)).append(')') }
                if (task.notes.isNotBlank()) sb.append("\n    ").append(task.notes)
                sb.append('\n')
            }
        }

        section(R.string.status_pending, "🔴", TaskStatus.PENDING)
        section(R.string.status_snoozed, "🟠", TaskStatus.SNOOZED)
        section(R.string.status_completed, "🟢", TaskStatus.COMPLETED)

        if (tasks.isEmpty()) sb.append('\n').append(context.getString(R.string.no_tasks_yet))
        return sb.toString()
    }

    fun share(context: Context, itemName: String, tasks: List<Task>) {
        val text = buildText(context, itemName, tasks)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, itemName)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.send_a_copy))
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(chooser)
    }
}
