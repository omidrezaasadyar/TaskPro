package com.taskpro.app.ui.screens.settings

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import android.net.Uri
import androidx.compose.ui.window.SecureFlagPolicy
import com.taskpro.app.backup.BackupScope
import com.taskpro.app.ui.screens.vault.VaultInput
import com.taskpro.app.ui.screens.vault.VaultChoice
import com.taskpro.app.ui.screens.vault.VaultToggle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskpro.app.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupSection(viewModel: SettingsViewModel) {
    val state by viewModel.backupState.collectAsStateWithLifecycle()
    val lastBackup by viewModel.lastBackupAt.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var destination by remember { mutableStateOf<Uri?>(null) }
    var source by remember { mutableStateOf<Uri?>(null) }
    var selectedScope by remember { mutableStateOf(BackupScope.FULL) }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        uri -> destination = uri
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        uri -> source = uri
    }
    BackHandler(enabled = state.busy || !state.ready) { }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.backup_restore), style = MaterialTheme.typography.titleMedium)
        VaultChoice(stringResource(R.string.backup_scope), selectedScope, listOf(
            BackupScope.FULL to stringResource(R.string.backup_full), BackupScope.TASKS to stringResource(R.string.backup_tasks),
            BackupScope.VAULT to stringResource(R.string.backup_vault))) { selectedScope = it }
        TextButton(
            enabled = state.ready && !state.busy,
            onClick = {
                val date = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                try { create.launch("Vaurivo-backup-$date.json") }
                catch (e: Exception) { viewModel.pickerFailed() }
            }
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Text(stringResource(R.string.backup_create), Modifier.padding(start = 8.dp))
        }
        TextButton(
            enabled = state.ready && !state.busy,
            onClick = {
                try { restore.launch(arrayOf("*/*")) }
                catch (e: Exception) { viewModel.pickerFailed() }
            }
        ) {
            Icon(Icons.Default.Restore, contentDescription = null)
            Text(stringResource(R.string.backup_restore_action), Modifier.padding(start = 8.dp))
        }
        Text(
            stringResource(R.string.backup_last, lastBackup?.let { DateFormat.getDateTimeInstance().format(Date(it)) }
                ?: stringResource(R.string.backup_never)),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(stringResource(R.string.backup_encrypted_note), style = MaterialTheme.typography.bodySmall)
        state.shareUri?.let { uri ->
            TextButton(enabled = !state.busy, onClick = {
                try {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newRawUri(context.getString(R.string.backup_share_label), uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.backup_share)))
                } catch (e: Exception) { viewModel.shareFailed() }
            }) {
                Icon(Icons.Default.Share, contentDescription = null)
                Text(stringResource(R.string.backup_share), Modifier.padding(start = 8.dp))
            }
        }
    }

    state.pending?.let { snapshot ->
        var tasks by remember(snapshot) { mutableStateOf(snapshot.tasks != null) }
        var vault by remember(snapshot) { mutableStateOf(snapshot.vault != null) }
        var master by remember(snapshot) { mutableStateOf("") }
        var useRecovery by remember(snapshot) { mutableStateOf(false) }
        var recovery by remember(snapshot) { mutableStateOf("") }
        var confirm by remember(snapshot) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
            title = { Text(stringResource(R.string.backup_restore_action)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.backup_metadata, DateFormat.getDateTimeInstance().format(Date(snapshot.createdAt)), snapshot.appVersion))
                    Text(stringResource(R.string.backup_replace_note))
                    if (snapshot.legacy) Text(stringResource(R.string.backup_legacy))
                    if (snapshot.tasks != null) VaultToggle(stringResource(R.string.workspace_tasks), tasks) { tasks = it }
                    if (snapshot.vault != null) VaultToggle(stringResource(R.string.vault_title), vault) { vault = it }
                    if (vault) {
                        VaultToggle(stringResource(R.string.vault_use_recovery), useRecovery) { useRecovery = it; master = ""; confirm = ""; recovery = "" }
                        if (useRecovery) {
                            Text(stringResource(R.string.vault_recovery_help), style = MaterialTheme.typography.bodySmall)
                            VaultInput(stringResource(R.string.vault_recovery_key), recovery, { recovery = it.take(128) }, secret = true)
                        }
                        VaultInput(stringResource(if (useRecovery) R.string.vault_new_password else R.string.vault_master), master, { master = it.take(1024) }, secret = true)
                        if (useRecovery) VaultInput(stringResource(R.string.vault_confirm_password), confirm, { confirm = it.take(1024) }, secret = true)
                    }
                }
            },
            confirmButton = { TextButton(enabled = !state.busy && (tasks || vault) && (!vault || (master.isNotBlank() && (!useRecovery || (recovery.isNotBlank() && master.length >= 12 && master == confirm)))), onClick = {
                val password = master.toCharArray(); val key = if (vault && useRecovery) recovery.toCharArray() else null
                master = ""; confirm = ""; recovery = ""; viewModel.confirmRestore(tasks, vault, password, key)
            }) {
                Text(stringResource(R.string.backup_replace))
            } },
            dismissButton = { TextButton(onClick = viewModel::cancelRestore) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (destination != null || source != null) {
        var password by remember(destination, source) { mutableStateOf("") }
        var confirm by remember(destination, source) { mutableStateOf("") }
        val creating = destination != null
        AlertDialog(onDismissRequest = { destination = null; source = null },
            properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
            title = { Text(stringResource(if (creating) R.string.backup_create else R.string.backup_open)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(if (creating) R.string.backup_encrypted_note else R.string.backup_open_note))
                VaultInput(stringResource(R.string.backup_password), password, { password = it.take(1024) }, secret = true)
                if (creating) com.taskpro.app.ui.screens.vault.PasswordStrength(password)
                if (creating) VaultInput(stringResource(R.string.vault_confirm_password), confirm, { confirm = it.take(1024) }, secret = true)
            } }, confirmButton = { TextButton(enabled = !creating || (password.length >= 12 && password == confirm), onClick = {
                val chars = password.toCharArray(); password = ""; confirm = ""
                if (creating) viewModel.createBackup(checkNotNull(destination), selectedScope, chars)
                else viewModel.selectBackup(checkNotNull(source), chars)
                destination = null; source = null
            }) { Text(stringResource(R.string.ok)) } }, dismissButton = { TextButton(onClick = { destination = null; source = null }) { Text(stringResource(R.string.cancel)) } })
    }
    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            title = { Text(stringResource(R.string.backup_restore)) },
            text = { Text(stringResource(message)) },
            confirmButton = { TextButton(onClick = viewModel::dismissMessage) { Text(stringResource(R.string.ok)) } }
        )
    }
    if (state.busy || !state.ready) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text(stringResource(R.string.backup_restore)) },
            text = {
                if (state.recoveryFailed) Text(stringResource(R.string.backup_recovery_error))
                else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.backup_working))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                if (state.recoveryFailed) TextButton(onClick = viewModel::recover) { Text(stringResource(R.string.backup_retry)) }
            }
        )
    }
}

@Composable
fun RestoreRecovery(failed: Boolean, onRetry: () -> Unit) {
    if (failed) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.backup_recovery_error))
            TextButton(onClick = onRetry) { Text(stringResource(R.string.backup_retry)) }
        }
    } else {
        Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text(stringResource(R.string.backup_working))
        }
    }
}
