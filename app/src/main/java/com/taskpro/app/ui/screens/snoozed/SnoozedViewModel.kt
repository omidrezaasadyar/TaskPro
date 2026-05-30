package com.taskpro.app.ui.screens.snoozed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskpro.app.data.Task
import com.taskpro.app.data.TaskRepository
import com.taskpro.app.data.TaskStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SnoozedViewModel(private val repository: TaskRepository) : ViewModel() {

    val tasks: StateFlow<List<Task>> =
        repository.observeAllByStatus(TaskStatus.SNOOZED)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Move a snoozed task back to pending. */
    fun resume(task: Task) {
        viewModelScope.launch { repository.setStatus(task, TaskStatus.PENDING) }
    }

    fun complete(task: Task) {
        viewModelScope.launch { repository.setStatus(task, TaskStatus.COMPLETED) }
    }

    fun delete(task: Task) {
        viewModelScope.launch { repository.deleteTask(task) }
    }
}
