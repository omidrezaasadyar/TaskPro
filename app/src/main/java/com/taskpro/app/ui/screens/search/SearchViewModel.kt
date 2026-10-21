package com.taskpro.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskpro.app.data.TaskRepository
import com.taskpro.app.data.TaskWithItemName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.CancellationException

data class TaskSearchState(val query: String = "", val results: List<TaskWithItemName> = emptyList(),
                           val loading: Boolean = false, val failed: Boolean = false)

/** Backs the menu search screen: filters tasks by name across all items. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val retries = MutableStateFlow(0)
    val state: StateFlow<TaskSearchState> =
        _query
            .debounce(200)
            .combine(retries) { q, _ -> q }
            .flatMapLatest { q ->
                val trimmed = q.trim()
                if (trimmed.isEmpty()) flowOf(TaskSearchState())
                else repository.searchTasks(trimmed).map { TaskSearchState(trimmed, it) }
                    .onStart { emit(TaskSearchState(trimmed, loading = true)) }
                    .catch {
                        if (it is CancellationException) throw it
                        emit(TaskSearchState(trimmed, failed = true))
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskSearchState())

    val results: StateFlow<List<TaskWithItemName>> = state.map { it.results }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun retry() { retries.value++ }

    fun onQueryChange(value: String) {
        _query.value = value
    }
}
