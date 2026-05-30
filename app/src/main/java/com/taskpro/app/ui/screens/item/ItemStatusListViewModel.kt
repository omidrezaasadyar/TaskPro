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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the per-item Completed/Snoozed list reached by tapping one of the two
 * summary boxes on an item's detail screen.
 */
class ItemStatusListViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskRepository
) : ViewModel() {

    val itemId: Long = checkNotNull(savedStateHandle["itemId"])
    val status: TaskStatus = TaskStatus.valueOf(checkNotNull(savedStateHandle["status"]))

    val item: StateFlow<Item?> = repository.observeItem(itemId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val tasks: StateFlow<List<Task>> = repository.observeTasks(itemId, status)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Send a task back to the pending list. */
    fun reopen(task: Task) {
        viewModelScope.launch { repository.setStatus(task, TaskStatus.PENDING) }
    }

    fun complete(task: Task) {
        viewModelScope.launch { repository.setStatus(task, TaskStatus.COMPLETED) }
    }

    fun delete(task: Task) {
        viewModelScope.launch { repository.deleteTask(task) }
    }
}
