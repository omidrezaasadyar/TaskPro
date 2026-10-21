package com.taskpro.app.backup

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.taskpro.app.BuildConfig
import com.taskpro.app.data.AppDatabase
import com.taskpro.app.data.RestoreJournal
import com.taskpro.app.data.TaskStatus
import com.taskpro.app.notification.ReminderScheduler
import com.taskpro.app.settings.BackgroundStyle
import com.taskpro.app.settings.SettingsRepository
import com.taskpro.app.settings.SettingsSnapshot
import com.taskpro.app.settings.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.IOException
import com.taskpro.app.vault.VaultRepository
import com.taskpro.app.vault.VaultStore
import com.taskpro.app.vault.VaultCrypto
import com.taskpro.app.vault.VaultDocument

class RestorePendingException(cause: Exception) : Exception(cause)

class BackupRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val vaults: VaultRepository = VaultRepository(database.vaultDao())
) {
    private val mutex = Mutex()

    suspend fun createWorkspace(uri: Uri, scope: BackupScope, password: CharArray) = withContext(Dispatchers.IO) {
        try {
            mutex.withLock {
                finishPendingRestore()
                val time = System.currentTimeMillis()
                val snapshot = database.withTransaction {
                    val tasks = if (scope != BackupScope.VAULT) BackupSnapshot(
                        database.itemDao().getAll(), database.taskDao().getAll(),
                        settings.snapshot().copy(lastBackupAt = time), time, BuildConfig.VERSION_NAME) else null
                    val vault = if (scope != BackupScope.TASKS) database.vaultDao().get()?.envelope else null
                    require(scope != BackupScope.VAULT || vault != null)
                    WorkspaceBackup(tasks, vault, time, BuildConfig.VERSION_NAME)
                }
                val bytes = WorkspaceBackupFormat.encode(snapshot, password)
                val output = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException()
                output.use { it.write(bytes); it.flush() }
                settings.recordBackup(time)
            }
        } finally { password.fill('\u0000') }
    }

    suspend fun readWorkspace(uri: Uri, password: CharArray): WorkspaceBackup = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openInputStream(uri) ?: throw IOException()
            val bytes = stream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= WorkspaceBackupFormat.MAX_BYTES)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            WorkspaceBackupFormat.decode(bytes, password)
        } finally { password.fill('\u0000') }
    }

    suspend fun restoreWorkspace(snapshot: WorkspaceBackup, tasks: Boolean, vault: Boolean, vaultPassword: CharArray, recoveryKey: CharArray? = null) =
        withContext(Dispatchers.IO + NonCancellable) {
            try {
                require(tasks || vault)
                require(vault || recoveryKey == null)
                val vaultEnvelope = if (vault && recoveryKey != null)
                    VaultCrypto.recover(requireNotNull(snapshot.vault), recoveryKey, vaultPassword.copyOf()) else snapshot.vault
                if (tasks) BackupFormat.encode(requireNotNull(snapshot.tasks))
                if (vault) {
                    val plain = VaultCrypto.open(requireNotNull(vaultEnvelope), vaultPassword, "vault")
                    try { VaultDocument.decode(plain) } finally { plain.fill(0) }
                }
                mutex.withLock {
                    vaults.mutex.withLock {
                        finishPendingRestore()
                        if (vault) vaults.lock()
                        database.withTransaction {
                            if (tasks) replaceTasks(requireNotNull(snapshot.tasks))
                            if (vault) {
                                val current = database.vaultDao().get()
                                if (current == null) database.vaultDao().insert(VaultStore(envelope = requireNotNull(vaultEnvelope)))
                                else check(database.vaultDao().update(requireNotNull(vaultEnvelope), current.revision) == 1)
                            }
                        }
                        try { finishPendingRestore(); vaults.refresh() }
                        catch (e: Exception) { throw RestorePendingException(e) }
                    }
                }
            } finally { vaultPassword.fill('\u0000'); recoveryKey?.fill('\u0000') }
        }

    private suspend fun replaceTasks(snapshot: BackupSnapshot) {
        val oldIds = JSONArray(database.taskDao().getAll().map { it.id }).toString()
        database.itemDao().deleteAll()
        database.itemDao().insertAll(snapshot.items)
        database.taskDao().insertAll(snapshot.tasks)
        database.restoreJournalDao().insert(RestoreJournal(
            theme = snapshot.settings.theme.name, background = snapshot.settings.background.name,
            lastBackupAt = snapshot.settings.lastBackupAt, oldTaskIds = oldIds
        ))
    }

    suspend fun create(uri: Uri) = withContext(Dispatchers.IO) {
        mutex.withLock {
            finishPendingRestore()
            val time = System.currentTimeMillis()
            val snapshot = database.withTransaction {
                BackupSnapshot(
                    database.itemDao().getAll(), database.taskDao().getAll(),
                    settings.snapshot().copy(lastBackupAt = time), time, BuildConfig.VERSION_NAME
                )
            }
            val bytes = BackupFormat.encode(snapshot)
            val stream = context.contentResolver.openOutputStream(uri, "wt")
                ?: throw IOException("Cannot open backup destination")
            stream.use { it.write(bytes); it.flush() }
            settings.recordBackup(time)
        }
    }

    suspend fun read(uri: Uri): BackupSnapshot = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open backup")
        val bytes = stream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (output.size() + count > BackupFormat.MAX_BYTES) throw InvalidBackupException()
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        BackupFormat.decode(bytes)
    }

    suspend fun restore(snapshot: BackupSnapshot) = withContext(Dispatchers.IO + NonCancellable) {
        mutex.withLock {
            finishPendingRestore()
            // Validate even callers that did not go through the document picker.
            BackupFormat.encode(snapshot)
            database.withTransaction {
                val oldIds = JSONArray(database.taskDao().getAll().map { it.id }).toString()
                database.itemDao().deleteAll()
                database.itemDao().insertAll(snapshot.items)
                database.taskDao().insertAll(snapshot.tasks)
                database.restoreJournalDao().insert(RestoreJournal(
                    theme = snapshot.settings.theme.name,
                    background = snapshot.settings.background.name,
                    lastBackupAt = snapshot.settings.lastBackupAt,
                    oldTaskIds = oldIds
                ))
            }
            try {
                finishPendingRestore()
            } catch (e: Exception) {
                // Rows are committed. Keep the journal for retry, never report a rollback.
                throw RestorePendingException(e)
            }
        }
    }

    suspend fun recover() = withContext(Dispatchers.IO + NonCancellable) {
        mutex.withLock { finishPendingRestore() }
    }

    private suspend fun finishPendingRestore() {
        val journal = database.restoreJournalDao().get() ?: return
        settings.replace(SettingsSnapshot(
            ThemeMode.valueOf(journal.theme), BackgroundStyle.valueOf(journal.background), journal.lastBackupAt
        ))
        val oldIds = JSONArray(journal.oldTaskIds)
        for (index in 0 until oldIds.length()) scheduler.cancel(oldIds.getLong(index))
        context.getSystemService(NotificationManager::class.java).cancelAll()
        val now = System.currentTimeMillis()
        database.taskDao().getAll().forEach { task ->
            if (task.status != TaskStatus.COMPLETED && task.dueAt != null && task.dueAt > now) {
                scheduler.schedule(task.id, task.title, task.dueAt)
            }
        }
        database.restoreJournalDao().clear()
    }
}
