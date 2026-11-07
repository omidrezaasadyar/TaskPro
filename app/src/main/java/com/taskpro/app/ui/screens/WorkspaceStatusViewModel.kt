package com.taskpro.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskpro.app.data.TaskRepository
import com.taskpro.app.settings.SettingsRepository
import com.taskpro.app.vault.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class WorkspaceStatus(
    val totalTasks: Int? = null,
    val lastBackupAt: Long? = null,
    val vaultAccounts: Int? = null,
    val vaultExists: Boolean? = null
)

/** Read-only Home summary. Locked Vault content is deliberately never cached outside the Vault. */
class WorkspaceStatusViewModel(
    tasks: TaskRepository,
    settings: SettingsRepository,
    private val vault: VaultRepository
) : ViewModel() {
    private val vaultKnown = MutableStateFlow(false)
    val status = combine(
        tasks.observeDashboardTasks().map { it.size },
        settings.lastBackupAt,
        vault.state,
        vaultKnown
    ) { total, backup, vaultState, known ->
        WorkspaceStatus(total, backup, vaultState.document?.records?.size,
            if (known) vaultState.exists else null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkspaceStatus())

    init {
        viewModelScope.launch {
            try { vault.refresh(); vaultKnown.value = true }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* A failed read leaves Vault status unknown. */ }
        }
    }
}
