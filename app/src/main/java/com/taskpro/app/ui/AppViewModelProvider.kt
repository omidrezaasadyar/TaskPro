package com.taskpro.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.taskpro.app.TaskProApp
import com.taskpro.app.ui.screens.completed.CompletedViewModel
import com.taskpro.app.ui.screens.home.HomeViewModel
import com.taskpro.app.ui.screens.item.ItemDetailViewModel
import com.taskpro.app.ui.screens.item.ItemStatusListViewModel
import com.taskpro.app.ui.screens.settings.SettingsViewModel
import com.taskpro.app.ui.screens.snoozed.SnoozedViewModel

/** Factories that wire each ViewModel to the app's repositories. */
object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer { HomeViewModel(app().container.taskRepository) }
        initializer { ItemDetailViewModel(this.createSavedStateHandle(), app().container.taskRepository) }
        initializer { ItemStatusListViewModel(this.createSavedStateHandle(), app().container.taskRepository) }
        initializer { CompletedViewModel(app().container.taskRepository) }
        initializer { SnoozedViewModel(app().container.taskRepository) }
        initializer { SettingsViewModel(app().container.settingsRepository) }
    }
}

private fun CreationExtras.app(): TaskProApp =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaskProApp)
