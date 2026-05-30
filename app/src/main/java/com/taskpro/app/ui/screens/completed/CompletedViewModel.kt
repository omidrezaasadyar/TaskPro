package com.taskpro.app.ui.screens.completed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskpro.app.data.Task
import com.taskpro.app.data.TaskRepository
import com.taskpro.app.data.TaskStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CompletedViewModel(private val repository: TaskRepository) : ViewModel() {

    val tasks: StateFlow<List<Task>> =
        repository.observeAllByStatus(TaskStatus.COMPLETED)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Reopen a completed task back to the pending list. */
    fun reopen(task: Task) {
        viewModelScope.launch { repository.setStatus(task, TaskStatus.PENDING) }
    }

    fun delete(task: Task) {
        viewModelScope.launch { repository.deleteTask(task) }
    }
}
