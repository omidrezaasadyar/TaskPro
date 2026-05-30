package com.taskpro.app.data

import com.taskpro.app.notification.ReminderScheduler
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for items and tasks. Also keeps reminder alarms in
 * sync with task state so callers never have to touch the scheduler directly.
 */
class TaskRepository(
    private val itemDao: ItemDao,
    private val taskDao: TaskDao,
    private val scheduler: ReminderScheduler
) {

    // ---- Items ----
    fun observeItemsWithCounts(): Flow<List<ItemWithTaskCounts>> = itemDao.observeItemsWithCounts()

    fun observeItem(id: Long): Flow<Item?> = itemDao.observeItem(id)

    suspend fun addItem(name: String, colorArgb: Int? = null): Long =
        itemDao.insert(Item(name = name, colorArgb = colorArgb))

    suspend fun updateItem(item: Item) = itemDao.update(item)

    suspend fun deleteItem(item: Item) {
        // Cancel any reminders belonging to this item's tasks before it cascades away.
        taskDao.getTasksForItem(item.id).forEach { scheduler.cancel(it.id) }
        itemDao.delete(item)
    }

    suspend fun getTasksForItem(itemId: Long): List<Task> = taskDao.getTasksForItem(itemId)

    // ---- Tasks ----
    fun observeTasks(itemId: Long, status: TaskStatus): Flow<List<Task>> =
        taskDao.observeTasks(itemId, status)

    fun observeAllByStatus(status: TaskStatus): Flow<List<Task>> =
        taskDao.observeAllByStatus(status)

    fun searchTasks(query: String): Flow<List<TaskWithItemName>> =
        taskDao.searchTasks(query)

    suspend fun addTask(itemId: Long, title: String, notes: String, dueAt: Long?) {
        val position = taskDao.nextPosition(itemId, TaskStatus.PENDING)
        val id = taskDao.insert(
            Task(itemId = itemId, title = title, notes = notes, dueAt = dueAt, position = position)
        )
        if (dueAt != null && dueAt > System.currentTimeMillis()) {
            scheduler.schedule(id, title, dueAt)
        }
    }

    suspend fun updateTask(task: Task) {
        taskDao.update(task)
        // Re-arm or clear the reminder to match the saved state.
        if (task.dueAt != null && task.status == TaskStatus.PENDING && task.dueAt > System.currentTimeMillis()) {
            scheduler.schedule(task.id, task.title, task.dueAt)
        } else {
            scheduler.cancel(task.id)
        }
    }

    suspend fun setStatus(task: Task, status: TaskStatus) {
        // Moving status changes which list it lives in; give it a fresh position there.
        val position = taskDao.nextPosition(task.itemId, status)
        val updated = task.copy(status = status, position = position)
        taskDao.update(updated)
        if (status == TaskStatus.PENDING && task.dueAt != null && task.dueAt > System.currentTimeMillis()) {
            scheduler.schedule(task.id, task.title, task.dueAt)
        } else {
            scheduler.cancel(task.id)
        }
    }

    suspend fun deleteTask(task: Task) {
        scheduler.cancel(task.id)
        taskDao.delete(task)
    }

    /** Marks a task complete by id — used from the full-screen reminder popup. */
    suspend fun markCompletedById(taskId: Long) {
        taskDao.getById(taskId)?.let { setStatus(it, TaskStatus.COMPLETED) }
    }

    /**
     * Snoozes a task by id and re-arms its reminder [minutes] from now — used
     * from the full-screen reminder popup's "snooze" action.
     */
    suspend fun snoozeByIdForMinutes(taskId: Long, minutes: Int) {
        val task = taskDao.getById(taskId) ?: return
        val newDue = System.currentTimeMillis() + minutes * 60_000L
        val position = taskDao.nextPosition(task.itemId, TaskStatus.SNOOZED)
        taskDao.update(task.copy(status = TaskStatus.SNOOZED, dueAt = newDue, position = position))
        // Snoozed reminders still ring; schedule the follow-up alert.
        scheduler.schedule(task.id, task.title, newDue)
    }

    suspend fun reorder(tasks: List<Task>) = taskDao.reorder(tasks)

    /** Re-arms alarms for all pending tasks with a future reminder (after reboot). */
    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        taskDao.getTasksWithReminders().forEach { task ->
            task.dueAt?.let { due ->
                if (due > now) scheduler.schedule(task.id, task.title, due)
            }
        }
    }
}
