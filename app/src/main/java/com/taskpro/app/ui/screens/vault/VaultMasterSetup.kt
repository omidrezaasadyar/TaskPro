package com.taskpro.app.ui.screens.vault

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.taskpro.app.R
import com.taskpro.app.vault.VaultRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException
import java.security.MessageDigest

@Composable
internal fun VaultMasterSetup(repository: VaultRepository, previousVault: Boolean, begin: () -> Unit, close: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hadPreviousVault = remember { previousVault }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    var pendingPassword by remember { mutableStateOf<CharArray?>(null) }
    var confirmReplacement by remember { mutableStateOf(false) }

    fun create(password: CharArray, destination: Uri? = null) {
        if (busy) { password.fill('\u0000'); return }
        begin()
        busy = true; error = null
        scope.launch {
            try {
                if (destination == null) repository.unlock(password, create = true)
                else repository.createAfterPreservingBackup(password) { bytes ->
                    val resolver = context.contentResolver
                    (resolver.openOutputStream(destination, "wt") ?: throw IOException()).use { it.write(bytes); it.flush() }
                    val digest = MessageDigest.getInstance("SHA-256")
                    var size = 0L
                    (resolver.openInputStream(destination) ?: throw IOException()).use { input ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            size += count
                            check(size <= bytes.size)
                            digest.update(buffer, 0, count)
                        }
                    }
                    check(size == bytes.size.toLong() && MessageDigest.isEqual(digest.digest(), MessageDigest.getInstance("SHA-256").digest(bytes)))
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = R.string.unlock_create_error }
            finally { password.fill('\u0000'); busy = false }
        }
    }

    val savePrevious = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val password = pendingPassword
        pendingPassword = null
        if (password != null) {
            if (uri == null) password.fill('\u0000') else create(password, uri)
        }
    }
    DisposableEffect(Unit) { onDispose { pendingPassword?.fill('\u0000') } }
    BackHandler { if (!busy) { pendingPassword?.fill('\u0000'); pendingPassword = null; close() } }

    VaultLogin(false, busy, error, null, unlock = { password ->
        if (hadPreviousVault) {
            pendingPassword?.fill('\u0000'); pendingPassword = password; confirmReplacement = true
        } else create(password)
    }, recover = { key, password -> key.fill('\u0000'); password.fill('\u0000') }, previousVault = hadPreviousVault,
        onCancel = { pendingPassword?.fill('\u0000'); pendingPassword = null; close() })

    if (confirmReplacement) AlertDialog(
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
        onDismissRequest = { confirmReplacement = false; pendingPassword?.fill('\u0000'); pendingPassword = null },
        title = { Text(stringResource(R.string.unlock_create_confirm_title)) },
        text = { Text(stringResource(R.string.unlock_create_confirm_text)) },
        confirmButton = { TextButton(onClick = {
            confirmReplacement = false
            try { savePrevious.launch("Vaurivo-previous-vault-${System.currentTimeMillis()}.json") }
            catch (_: Exception) { pendingPassword?.fill('\u0000'); pendingPassword = null; error = R.string.unlock_create_error }
        }) { Text(stringResource(R.string.unlock_save_previous)) } },
        dismissButton = { TextButton(onClick = {
            confirmReplacement = false; pendingPassword?.fill('\u0000'); pendingPassword = null
        }) { Text(stringResource(R.string.cancel)) } }
    )
}
