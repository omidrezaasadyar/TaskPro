package com.taskpro.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskpro.app.data.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(repository: TaskRepository, moments: Flow<DashboardMoment> = dashboardClock()) : ViewModel() {
    private val retries = MutableStateFlow(0)
    val state = retries.flatMapLatest {
        combine(repository.observeDashboardTasks(), moments) { rows, moment ->
            DashboardUiState(dashboardSummary(rows, moment), loading = false)
        }.onStart { emit(DashboardUiState()) }.catch {
            if (it is CancellationException) throw it
            emit(DashboardUiState(loading = false, failed = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0, replayExpirationMillis = 0), DashboardUiState())

    fun retry() { retries.value++ }
}
