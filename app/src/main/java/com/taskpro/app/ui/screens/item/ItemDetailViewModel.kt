package com.taskpro.app.ui.screens.item

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskpro.app.data.Item
import com.taskpro.app.data.Task
import com.taskpro.app.data.TaskRepository
import com.taskpro.app.data.TaskStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItemDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskRepository
) : ViewModel() {

    val itemId: Long = checkNotNull(savedStateHandle["itemId"])

    val item: StateFlow<Item?> = repository.observeItem(itemId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Pending tasks in manual-priority order; the reorderable list. */
    val pendingTasks: StateFlow<List<Task>> =
        repository.observeTasks(itemId, TaskStatus.PENDING)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Live counts for this item's Completed / Snoozed summary boxes. */
    val completedCount: StateFlow<Int> =
        repository.observeTasks(itemId, TaskStatus.COMPLETED)
            .map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val snoozedCount: StateFlow<Int> =
        repository.observeTasks(itemId, TaskStatus.SNOOZED)
            .map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun addTask(title: String, notes: String, dueAt: Long?) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.addTask(itemId, trimmed, notes.trim(), dueAt) }
    }

    fun complete(task: Task) = setStatus(task, TaskStatus.COMPLETED)
    fun snooze(task: Task) = setStatus(task, TaskStatus.SNOOZED)

    private fun setStatus(task: Task, status: TaskStatus) {
        viewModelScope.launch { repository.setStatus(task, status) }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    /** Persists a new ordering produced by drag-to-reorder. */
    fun persistOrder(tasks: List<Task>) {
        viewModelScope.launch { repository.reorder(tasks) }
    }

    suspend fun tasksForExport(): List<Task> = repository.getTasksForItem(itemId)
}
