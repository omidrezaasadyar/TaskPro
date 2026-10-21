package com.taskpro.app.ui.screens.vault

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.taskpro.app.BuildConfig
import com.taskpro.app.R
import com.taskpro.app.ui.screens.WorkspaceBackdrop
import com.taskpro.app.ui.theme.LocalVaurivoColors
import com.taskpro.app.ui.theme.VaurivoLayout
import com.taskpro.app.ui.theme.VaurivoShapes

@Composable
fun VaultLogin(exists: Boolean, busy: Boolean, error: Int?, notice: Int?,
               unlock: (CharArray) -> Unit, recover: (CharArray, CharArray) -> Unit,
               previousVault: Boolean = false, onCancel: (() -> Unit)? = null) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var forgot by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val alpha by animateFloatAsState(if (entered) 1f else 0f, tween(240), label = "vaultLoginEntrance")
    val palette = LocalVaurivoColors.current
    val accent = palette.vaultAccent
    WorkspaceBackdrop(Modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.Center).widthIn(max = 500.dp).fillMaxWidth().imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp).graphicsLayer { this.alpha = alpha },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Surface(shape = VaurivoShapes.card, color = palette.surface,
                border = BorderStroke(1.dp, palette.borderSubtle)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(64.dp).background(palette.vaultContainer, VaurivoShapes.card), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.VerifiedUser, null, Modifier.size(32.dp), tint = accent)
                    }
                    Text(stringResource(if (exists) R.string.vault_locked else R.string.unlock_create_master), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.vault_login_subtitle), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    if (!exists) Text(stringResource(R.string.vault_password_notice), style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary)
                    if (previousVault) Text(stringResource(R.string.unlock_existing_vault_note), style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary)
                    VaultInput(stringResource(if (exists) R.string.vault_master else R.string.vault_new_password), password, { password = it.take(1024) }, secret = true)
                    if (!exists) {
                        PasswordStrength(password)
                        VaultInput(stringResource(R.string.vault_confirm_password), confirm, { confirm = it.take(1024) }, secret = true)
                        Text(stringResource(if (confirm.isNotEmpty() && password != confirm) R.string.vault_password_mismatch else R.string.vault_password_rule),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (error != null && !forgot) Text(stringResource(error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    if (notice != null) Text(stringResource(notice), style = MaterialTheme.typography.bodySmall)
                    Button(enabled = !busy && password.isNotEmpty() && (exists || (password.length >= 12 && password == confirm)),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = palette.onAccent),
                        modifier = Modifier.fillMaxWidth().heightIn(min = VaurivoLayout.primaryActionHeight), shape = VaurivoShapes.control, onClick = {
                            val chars = password.toCharArray(); password = ""; confirm = ""; unlock(chars)
                        }) {
                        Icon(Icons.Default.LockOpen, null, Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
                        Text(stringResource(if (exists) R.string.vault_unlock else R.string.unlock_create_master), textAlign = TextAlign.Center)
                    }
                    if (onCancel != null) TextButton(enabled = !busy, onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                    if (exists) TextButton(enabled = !busy, colors = ButtonDefaults.textButtonColors(contentColor = accent), onClick = { password = ""; forgot = true }) { Text(stringResource(R.string.vault_forgot)) }
                }
            }
            Text(stringResource(R.string.vault_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (forgot) RecoveryForm(busy, error, { forgot = false }, recover)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecoveryForm(busy: Boolean, error: Int?, close: () -> Unit, recover: (CharArray, CharArray) -> Unit) {
    var key by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = { if (!busy) close() }, properties = DialogProperties(usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)) {
        val palette = LocalVaurivoColors.current
        Scaffold(containerColor = palette.canvas, topBar = { TopAppBar(title = { Text(stringResource(R.string.vault_recovery_title)) }, navigationIcon = {
            IconButton(enabled = !busy, onClick = close) { Icon(Icons.Default.Close, stringResource(R.string.cancel)) }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.canvas)) }, bottomBar = {
            Surface(color = palette.surface) {
                Button(enabled = !busy && key.isNotBlank() && password.length >= 12 && password == confirm,
                    onClick = { val k = key.toCharArray(); val p = password.toCharArray(); key = ""; password = ""; confirm = ""; attempted = true; recover(k, p) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                        .heightIn(min = VaurivoLayout.primaryActionHeight), shape = VaurivoShapes.control) {
                    Text(stringResource(R.string.vault_recovery_reset))
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.vault_recovery_help))
                VaultInput(stringResource(R.string.vault_recovery_key), key, { key = it.take(128) }, secret = true)
                VaultInput(stringResource(R.string.vault_new_password), password, { password = it.take(1024) }, secret = true)
                PasswordStrength(password)
                VaultInput(stringResource(R.string.vault_confirm_password), confirm, { confirm = it.take(1024) }, secret = true)
                Text(stringResource(if (confirm.isNotEmpty() && password != confirm) R.string.vault_password_mismatch else R.string.vault_password_rule), style = MaterialTheme.typography.bodySmall)
                if (attempted && error != null) Text(stringResource(R.string.vault_recovery_error), color = MaterialTheme.colorScheme.error)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoverySetup(busy: Boolean, close: () -> Unit, copy: (String) -> Unit,
                  initialKey: String? = null, view: ((CharArray, (String) -> Unit) -> Unit)? = null,
                  generate: (CharArray, (String) -> Unit) -> Unit) {
    var password by remember { mutableStateOf("") }; var key by remember { mutableStateOf(initialKey) }; var saved by remember { mutableStateOf(false) }
    val canClose = !busy && (key == null || saved)
    val palette = LocalVaurivoColors.current
    val keyDisplayedLabel = stringResource(R.string.vault_recovery_key_displayed)
    Dialog(onDismissRequest = { if (canClose) close() }, properties = DialogProperties(usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)) {
        WorkspaceBackdrop(Modifier.fillMaxSize()) {
        Scaffold(containerColor = Color.Transparent, modifier = Modifier.vaultActivity(), topBar = { TopAppBar(title = { Text(stringResource(R.string.vault_recovery_setup)) }, navigationIcon = {
            IconButton(enabled = canClose, onClick = close) { Icon(Icons.Default.Close, stringResource(R.string.cancel)) }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.surface)) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (key == null) {
                    Text(stringResource(R.string.vault_recovery_legacy))
                    VaultInput(stringResource(R.string.vault_old_password), password, { password = it.take(1024) }, secret = true)
                    if (view != null) OutlinedButton(enabled = !busy && password.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = VaurivoLayout.minimumTouchTarget), onClick = {
                        val p = password.toCharArray(); password = ""; view(p) { key = it }
                    }) { Text(stringResource(R.string.vault_recovery_view)) }
                    Button(enabled = !busy && password.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = VaurivoLayout.primaryActionHeight), onClick = { val p = password.toCharArray(); password = ""; generate(p) { key = it } }) {
                        Text(stringResource(R.string.vault_recovery_generate))
                    }
                } else {
                    Text(stringResource(R.string.vault_recovery_save_note))
                    Text(checkNotNull(key), Modifier.clearAndSetSemantics { contentDescription = keyDisplayedLabel },
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        textDirection = androidx.compose.ui.text.style.TextDirection.Ltr))
                    TextButton(onClick = { copy(checkNotNull(key)) }, modifier = Modifier.heightIn(min = VaurivoLayout.minimumTouchTarget)) {
                        Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.vault_copy))
                    }
                    VaultToggle(stringResource(R.string.vault_recovery_saved), saved) { saved = it }
                    Button(enabled = saved && !busy, onClick = close, modifier = Modifier.fillMaxWidth().heightIn(min = VaurivoLayout.primaryActionHeight)) {
                        Text(stringResource(R.string.ok))
                    }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
        }
    }
}
