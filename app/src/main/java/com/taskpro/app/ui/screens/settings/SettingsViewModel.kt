package com.taskpro.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.taskpro.app.R
import com.taskpro.app.backup.BackupRepository
import com.taskpro.app.backup.WorkspaceBackup
import com.taskpro.app.backup.BackupScope
import com.taskpro.app.backup.InvalidBackupException
import com.taskpro.app.backup.UnsupportedBackupException
import com.taskpro.app.backup.RestorePendingException
import com.taskpro.app.settings.BackgroundStyle
import com.taskpro.app.settings.SettingsRepository
import com.taskpro.app.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class BackupUiState(
    val busy: Boolean = false,
    val ready: Boolean = false,
    val recoveryFailed: Boolean = false,
    val restoreCommitted: Boolean = false,
    val pending: WorkspaceBackup? = null,
    val shareUri: Uri? = null,
    val message: Int? = null
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val backups: BackupRepository
) : ViewModel() {

    private val mutableBackupState = MutableStateFlow(BackupUiState())
    val backupState = mutableBackupState.asStateFlow()
    val lastBackupAt = repository.lastBackupAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init { recover() }

    fun recover() {
        if (mutableBackupState.value.busy) return
        mutableBackupState.update { it.copy(busy = true, recoveryFailed = false) }
        viewModelScope.launch {
            try {
                backups.recover()
                mutableBackupState.update {
                    it.copy(ready = true, restoreCommitted = false,
                        message = if (it.restoreCommitted) R.string.backup_restore_success else it.message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableBackupState.update { it.copy(ready = false, recoveryFailed = true) }
            } finally {
                mutableBackupState.update { it.copy(busy = false) }
            }
        }
    }

    fun createBackup(uri: Uri, scope: BackupScope, password: CharArray) = operation(R.string.backup_write_error, password) {
        backups.createWorkspace(uri, scope, password)
        mutableBackupState.update { it.copy(shareUri = uri, message = R.string.backup_success) }
    }

    fun selectBackup(uri: Uri, password: CharArray) = operation(R.string.backup_auth_error, password) {
        val snapshot = backups.readWorkspace(uri, password)
        mutableBackupState.update { it.copy(pending = snapshot) }
    }

    fun cancelRestore() { mutableBackupState.update { it.copy(pending = null) } }
    fun dismissMessage() { mutableBackupState.update { it.copy(message = null) } }
    fun shareFailed() { mutableBackupState.update { it.copy(message = R.string.backup_share_error) } }
    fun pickerFailed() { mutableBackupState.update { it.copy(message = R.string.backup_picker_error) } }

    fun confirmRestore(tasks: Boolean, vault: Boolean, password: CharArray, recovery: CharArray? = null) {
        val snapshot = mutableBackupState.value.pending ?: run { password.fill('\u0000'); recovery?.fill('\u0000'); return }
        operation(R.string.backup_auth_error, password, recovery) {
            backups.restoreWorkspace(snapshot, tasks, vault, password, recovery)
            mutableBackupState.update { it.copy(pending = null, message = R.string.backup_restore_success, shareUri = null) }
        }
    }

    private fun operation(errorMessage: Int, password: CharArray, recovery: CharArray? = null, block: suspend () -> Unit) {
        if (mutableBackupState.value.busy || !mutableBackupState.value.ready) { password.fill('\u0000'); recovery?.fill('\u0000'); return }
        mutableBackupState.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: RestorePendingException) {
                mutableBackupState.update { it.copy(ready = false, recoveryFailed = true, restoreCommitted = true) }
            } catch (e: Exception) {
                val message = when (e) {
                    is InvalidBackupException -> R.string.backup_invalid
                    is UnsupportedBackupException -> R.string.backup_unsupported
                    else -> errorMessage
                }
                mutableBackupState.update { it.copy(message = message) }
            } finally {
                password.fill('\u0000')
                recovery?.fill('\u0000')
                mutableBackupState.update { it.copy(busy = false) }
            }
        }
    }

    val themeMode: StateFlow<ThemeMode> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val backgroundStyle: StateFlow<BackgroundStyle> = repository.backgroundStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackgroundStyle.GRADIENT)

    fun setThemeMode(mode: ThemeMode) {
        if (mutableBackupState.value.busy || !mutableBackupState.value.ready) return
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setBackgroundStyle(style: BackgroundStyle) {
        if (mutableBackupState.value.busy || !mutableBackupState.value.ready) return
        viewModelScope.launch { repository.setBackgroundStyle(style) }
    }
}
