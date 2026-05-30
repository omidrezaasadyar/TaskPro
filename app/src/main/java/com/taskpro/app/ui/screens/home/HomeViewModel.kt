package com.taskpro.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskpro.app.data.Item
import com.taskpro.app.data.ItemWithTaskCounts
import com.taskpro.app.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: TaskRepository) : ViewModel() {

    val items: StateFlow<List<ItemWithTaskCounts>> =
        repository.observeItemsWithCounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addItem(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.addItem(trimmed) }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    fun renameItem(item: Item, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.updateItem(item.copy(name = trimmed)) }
    }

    /** Returns the item plus all its tasks for the "send a copy" feature. */
    suspend fun exportData(itemId: Long) = repository.getTasksForItem(itemId)
}
