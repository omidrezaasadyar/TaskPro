package com.taskpro.app.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskpro.app.data.*
import com.taskpro.app.ui.screens.dashboard.dashboardClock
import kotlinx.coroutines.flow.*

data class TasksState(
    val rows: List<TaskWithItemName> = emptyList(),
    val groups: List<ItemWithTaskCounts> = emptyList(),
    val now: Long = System.currentTimeMillis(),
    val zoneId: String = java.util.TimeZone.getDefault().id,
    val loading: Boolean = true,
    val failed: Boolean = false
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TasksViewModel(private val repository: TaskRepository) : ViewModel() {
    private val retry = MutableStateFlow(0)
    val state = retry.flatMapLatest {
        combine(repository.observeDashboardTasks(), repository.observeItemsWithCounts(), dashboardClock()) { rows, groups, clock ->
            TasksState(rows, groups, clock.now, clock.zoneId, loading = false)
        }.onStart { emit(TasksState()) }.catch { emit(TasksState(loading = false, failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0, 0), TasksState())

    fun retry() { retry.value++ }
    suspend fun save(task: Task?, groupId: Long?, title: String, notes: String, dueAt: Long?) {
        require(title.isNotBlank())
        if (task == null) {
            requireNotNull(groupId)
            repository.addTask(groupId, title.trim(), notes.trim(), dueAt)
        } else repository.editPendingTask(task.id, title.trim(), notes.trim(), dueAt)
    }
    suspend fun status(task: Task, status: TaskStatus) = repository.setStatusById(task.id, status)
    suspend fun delete(task: Task) = repository.deleteTask(task)
}
