package com.taskpro.app.vault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.room.Room
import com.taskpro.app.backup.*
import com.taskpro.app.data.*
import com.taskpro.app.notification.ReminderScheduler
import com.taskpro.app.settings.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VaultIntegrationTest {
    private lateinit var db: AppDatabase
    private lateinit var vault: VaultRepository
    private lateinit var backups: BackupRepository
    private lateinit var prefs: SettingsRepository
    private val master = "synthetic master phrase"
    private val backupPass = "synthetic backup phrase"
    @Before fun setup() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        vault = VaultRepository(db.vaultDao())
        prefs = SettingsRepository(context)
        backups = BackupRepository(context, db, prefs, ReminderScheduler(context), vault)
        db.itemDao().insert(Item(1, "Existing group"))
        db.taskDao().insert(Task(1, 1, "Existing task"))
        vault.unlock(master.toCharArray(), create = true)
        vault.edit { it.copy(records = listOf(VaultRecord(title = "Private title", values = mapOf("password" to "private-password")))) }
    }
    @After fun teardown() { vault.lock(); db.close() }
    @Test fun newMasterPreservesPreviousVaultInCompatibleBackupAndDoesNotChangeTasks() = runBlocking {
        val previous = db.vaultDao().get()!!.envelope
        val originalTasks = db.taskDao().getAll()
        val originalItems = db.itemDao().getAll()
        vault.lock()
        var saved: ByteArray? = null
        vault.createAfterPreservingBackup("synthetic next master".toCharArray()) { saved = it.copyOf() }
        val backup = WorkspaceBackupFormat.decode(checkNotNull(saved), "synthetic next master".toCharArray())
        assertNull(backup.tasks)
        assertEquals(previous, backup.vault)
        val plain = VaultCrypto.open(checkNotNull(backup.vault), master.toCharArray(), "vault")
        try { assertEquals("Private title", VaultDocument.decode(plain).records.single().title) } finally { plain.fill(0) }
        assertTrue(vault.state.value.document!!.records.isEmpty())
        assertNotNull(vault.state.value.pendingRecovery)
        assertEquals(originalTasks, db.taskDao().getAll())
        assertEquals(originalItems, db.itemDao().getAll())
        vault.confirmRecoverySaved(); vault.lock(); vault.unlock("synthetic next master".toCharArray())
        assertTrue(vault.state.value.document!!.records.isEmpty())
    }
    @Test fun failedPreservationAndStaleSnapshotCannotReplaceTheVault() = runBlocking {
        val previous = db.vaultDao().get()
        vault.lock()
        val password = "synthetic next master".toCharArray()
        try { vault.createAfterPreservingBackup(password) { throw java.io.IOException("Synthetic file failure") }; fail() }
        catch (_: java.io.IOException) { }
        assertTrue(password.all { it == '\u0000' })
        assertEquals(previous, db.vaultDao().get())
        try {
            vault.createAfterPreservingBackup("synthetic next master".toCharArray()) {
                check(db.vaultDao().update(previous!!.envelope, previous.revision) == 1)
            }
            fail()
        } catch (_: IllegalStateException) { }
        assertEquals(previous!!.envelope, db.vaultDao().get()!!.envelope)
        vault.unlock(master.toCharArray())
        assertEquals("Private title", vault.state.value.document!!.records.single().title)
    }
    @Test fun automaticRecoveryPersistsConfirmationAndRequiresAuthenticationForViewing() = runBlocking {
        val recovery = checkNotNull(vault.state.value.pendingRecovery)
        val original = vault.state.value.document
        vault.lock()
        vault.unlock(master.toCharArray())
        assertEquals(recovery, vault.state.value.pendingRecovery)
        vault.confirmRecoverySaved()
        vault.lock()
        vault.unlock(master.toCharArray())
        assertNull(vault.state.value.pendingRecovery)
        assertEquals(recovery, vault.setupRecovery(master.toCharArray(), viewOnly = true))
        try { vault.setupRecovery("wrong password".toCharArray(), viewOnly = true); fail() } catch (_: Exception) { }
        assertEquals(original, vault.state.value.document)
        val oldKey = VaultCrypto.unwrap(db.vaultDao().get()!!.envelope, master.toCharArray(), "vault")
        vault.setupRecovery(master.toCharArray())
        try {
            assertThrows(Exception::class.java) { VaultCrypto.decrypt(runBlocking { db.vaultDao().get() }!!.envelope, oldKey, "vault") }
        } finally { oldKey.fill(0) }
        Unit
    }
    @Test fun recoveryPreservesDataRotatesDataKeyAndConsumesRecoveryAccess() = runBlocking {
        val original = vault.state.value.document
        val recovery = vault.setupRecovery(master.toCharArray())
        val before = db.vaultDao().get()!!.envelope
        assertFalse(before.contains(recovery.replace("-", "")))
        val oldKey = VaultCrypto.unwrap(before, master.toCharArray(), "vault")
        vault.lock()
        vault.recoverPassword(recovery.toCharArray(), "new synthetic master".toCharArray())
        assertNull(vault.state.value.document)
        val after = db.vaultDao().get()!!.envelope
        assertThrows(Exception::class.java) { VaultCrypto.decrypt(after, oldKey, "vault") }
        oldKey.fill(0)
        try { vault.unlock(master.toCharArray()); fail() } catch (_: Exception) { }
        try { vault.recoverPassword(recovery.toCharArray(), "another new master".toCharArray()); fail() } catch (_: Exception) { }
        vault.unlock("new synthetic master".toCharArray())
        assertEquals(original, vault.state.value.document)
    }
    @Test fun recoverySetupRequiresUnlockAndReauthenticationAndRotationInvalidatesOldKey() = runBlocking {
        val before = db.vaultDao().get()
        try { vault.setupRecovery("incorrect password".toCharArray()); fail() } catch (_: Exception) { }
        assertEquals(before, db.vaultDao().get())
        val first = vault.setupRecovery(master.toCharArray())
        val second = vault.setupRecovery(master.toCharArray())
        vault.lock()
        try { vault.setupRecovery(master.toCharArray()); fail() } catch (_: Exception) { }
        val saved = db.vaultDao().get()
        try { vault.recoverPassword(first.toCharArray(), "new synthetic master".toCharArray()); fail() } catch (_: Exception) { }
        assertEquals(saved, db.vaultDao().get())
        vault.recoverPassword(second.toCharArray(), "new synthetic master".toCharArray())
        vault.unlock("new synthetic master".toCharArray())
        assertEquals("Private title", vault.state.value.document!!.records.single().title)
    }
    @Test fun recoverySurvivesOrdinaryEditsPasswordChangeAndBackupRoundTrip() = runBlocking {
        val recovery = vault.setupRecovery(master.toCharArray())
        vault.edit { it.copy(autoLockSeconds = 300) }
        vault.changePassword(master.toCharArray(), "changed synthetic master".toCharArray())
        val original = snapshot()
        val exported = WorkspaceBackupFormat.encode(original, backupPass.toCharArray())
        val imported = WorkspaceBackupFormat.decode(exported, backupPass.toCharArray())
        assertEquals(original.vault, imported.vault)
        vault.recoverPassword(recovery.toCharArray(), "recovered synthetic master".toCharArray())
        vault.unlock("recovered synthetic master".toCharArray())
        assertEquals(300, vault.state.value.document!!.autoLockSeconds)
    }
    @Test fun absentWrongAndTamperedRecoveryNeverModifyVault() = runBlocking {
        var before = db.vaultDao().get()
        try { vault.recoverPassword(VaultCrypto.newRecoveryKey().toCharArray(), "new synthetic master".toCharArray()); fail() } catch (_: Exception) { }
        assertEquals(before, db.vaultDao().get())
        val recovery = vault.setupRecovery(master.toCharArray())
        before = db.vaultDao().get()
        try { vault.recoverPassword(VaultCrypto.newRecoveryKey().toCharArray(), "new synthetic master".toCharArray()); fail() } catch (_: Exception) { }
        assertEquals(before, db.vaultDao().get())
        val json = org.json.JSONObject(before!!.envelope)
        val wrapped = json.getJSONObject("recovery").getString("key")
        json.getJSONObject("recovery").put("key", (if (wrapped.first() == '0') "1" else "0") + wrapped.drop(1))
        assertThrows(Exception::class.java) { VaultCrypto.recover(json.toString(), recovery.toCharArray(), "new synthetic master".toCharArray()) }
        Unit
    }
    @Test fun backupRecoveryPreservesUnselectedTasksAndRollsBackOnFailure() = runBlocking {
        val recovery = vault.setupRecovery(master.toCharArray())
        val exported = WorkspaceBackupFormat.encode(snapshot(), backupPass.toCharArray())
        val imported = WorkspaceBackupFormat.decode(exported, backupPass.toCharArray())
        val tasks = db.taskDao().getAll()
        val before = db.vaultDao().get()
        try { backups.restoreWorkspace(imported, false, true, "new backup master".toCharArray(), VaultCrypto.newRecoveryKey().toCharArray()); fail() } catch (_: Exception) { }
        assertEquals(before, db.vaultDao().get()); assertEquals(tasks, db.taskDao().getAll())
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_recovery BEFORE UPDATE ON vault_state BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END")
        try { backups.restoreWorkspace(imported, true, true, "new backup master".toCharArray(), recovery.toCharArray()); fail() } catch (_: Exception) { }
        assertEquals(before, db.vaultDao().get()); assertEquals(tasks, db.taskDao().getAll())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_recovery")
        backups.restoreWorkspace(imported, false, true, "new backup master".toCharArray(), recovery.toCharArray())
        assertEquals(tasks, db.taskDao().getAll())
        vault.unlock("new backup master".toCharArray())
        assertEquals("Private title", vault.state.value.document!!.records.single().title)
    }
    private suspend fun snapshot(): WorkspaceBackup {
        return WorkspaceBackup(BackupSnapshot(listOf(Item(50, "Restored group")), listOf(Task(50, 50, "Restored task")),
            SettingsSnapshot(ThemeMode.DARK, BackgroundStyle.PLAIN, null), 1234, "2.0"),
            db.vaultDao().get()!!.envelope, 1234, "2.0")
    }
    @Test fun noPlaintextAndMasterPasswordChangeAndLock() = runBlocking {
        val encoded = db.vaultDao().get()!!.envelope
        assertFalse(encoded.contains("Private title")); assertFalse(encoded.contains("private-password"))
        vault.lock(); assertNull(vault.state.value.document)
        try { vault.edit { it }; fail() } catch (_: IllegalStateException) { }
        vault.unlock(master.toCharArray())
        vault.changePassword(master.toCharArray(), "another synthetic phrase".toCharArray())
        assertNull(vault.state.value.document)
        try { vault.unlock(master.toCharArray()); fail() } catch (_: Exception) { }
        vault.unlock("another synthetic phrase".toCharArray())
        assertEquals("Private title", vault.state.value.document!!.records.single().title)
    }
    @Test fun inactivityLocksAndUserActivityResetsTimer() = runBlocking {
        vault.edit { it.copy(autoLockSeconds = 30) }
        ShadowSystemClock.advanceBy(Duration.ofSeconds(20)); vault.touch()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(20)); vault.checkInactivity()
        assertNotNull(vault.state.value.document)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(11)); vault.checkInactivity()
        assertNull(vault.state.value.document)
    }
    @Test fun largeVaultUsesBoundedRowsAndRoundTrips() = runBlocking {
        val records = (1..300).map { VaultRecord(title = "Synthetic $it", values = mapOf("notes" to "x".repeat(4096))) }
        vault.edit { it.copy(records = records) }
        db.openHelper.readableDatabase.query("SELECT MAX(length(ciphertext)) FROM vault_chunks").use {
            assertTrue(it.moveToFirst()); assertTrue(it.getLong(0) <= 256 * 1024)
        }
        vault.lock(); vault.unlock(master.toCharArray())
        assertEquals(records, vault.state.value.document!!.records)
    }
    @Test fun encryptedUnifiedRoundTripAndLegacyCompatibility() = runBlocking {
        val original = snapshot()
        val encrypted = WorkspaceBackupFormat.encode(original, backupPass.toCharArray())
        assertFalse(String(encrypted).contains("Restored task"))
        assertFalse(String(encrypted).contains("private-password"))
        val decoded = WorkspaceBackupFormat.decode(encrypted, backupPass.toCharArray())
        assertEquals(original, decoded)
        backups.restoreWorkspace(decoded, tasks = true, vault = true, master.toCharArray())
        assertEquals("Restored task", db.taskDao().getAll().single().title)
        assertNull(vault.state.value.document)
        vault.unlock(master.toCharArray())
        assertEquals("Private title", vault.state.value.document!!.records.single().title)
        val legacy = WorkspaceBackupFormat.decode(BackupFormat.encode(original.tasks!!), charArrayOf())
        assertTrue(legacy.legacy); assertNull(legacy.vault)
    }
    @Test fun exportsEveryScopeAndRestoresFullFileIntoFreshInstallation() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val records = vault.state.value.document!!.records
        for (scope in BackupScope.entries) {
            val file = java.io.File(context.cacheDir, "workspace-${scope.name}.json")
            val uri = android.net.Uri.fromFile(file)
            backups.createWorkspace(uri, scope, backupPass.toCharArray())
            assertFalse(file.readText().contains("private-password"))
            val decoded = backups.readWorkspace(uri, backupPass.toCharArray())
            assertEquals(scope != BackupScope.VAULT, decoded.tasks != null)
            assertEquals(scope != BackupScope.TASKS, decoded.vault != null)
            if (scope == BackupScope.FULL) {
                val fresh = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
                val freshVault = VaultRepository(fresh.vaultDao())
                try {
                    BackupRepository(context, fresh, prefs, ReminderScheduler(context), freshVault)
                        .restoreWorkspace(decoded, true, true, master.toCharArray())
                    freshVault.unlock(master.toCharArray())
                    assertEquals(records, freshVault.state.value.document!!.records)
                    assertEquals(db.taskDao().getAll(), fresh.taskDao().getAll())
                } finally { freshVault.lock(); fresh.close() }
            }
        }
    }
    @Test fun selectiveRestoreDoesNotTouchOtherModule() = runBlocking {
        val original = snapshot()
        val taskBefore = db.taskDao().getAll()
        val prefsBefore = prefs.snapshot()
        backups.restoreWorkspace(original, tasks = false, vault = true, master.toCharArray())
        assertEquals(taskBefore, db.taskDao().getAll()); assertEquals(prefsBefore, prefs.snapshot())
        val vaultBefore = db.vaultDao().get()
        backups.restoreWorkspace(original, tasks = true, vault = false, charArrayOf())
        assertEquals(vaultBefore, db.vaultDao().get())
    }
    @Test fun wrongPasswordAndCorruptionNeverImport() = runBlocking {
        val original = snapshot()
        val before = db.taskDao().getAll(); val beforeVault = db.vaultDao().get()
        try { backups.restoreWorkspace(original, true, true, "incorrect master".toCharArray()); fail() } catch (_: Exception) { }
        assertEquals(before, db.taskDao().getAll()); assertEquals(beforeVault, db.vaultDao().get())
        val bytes = WorkspaceBackupFormat.encode(original, backupPass.toCharArray())
        assertThrows(Exception::class.java) { WorkspaceBackupFormat.decode(bytes, "wrong backup password".toCharArray()) }
        assertThrows(Exception::class.java) { WorkspaceBackupFormat.decode(bytes.copyOf(bytes.size / 2), backupPass.toCharArray()) }
        Unit
    }
    @Test fun combinedRestoreRollsBackBothModulesOnDatabaseFailure() = runBlocking {
        val original = snapshot()
        val before = db.taskDao().getAll(); val beforeVault = db.vaultDao().get(); val beforePrefs = prefs.snapshot()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_vault BEFORE UPDATE ON vault_state BEGIN SELECT RAISE(ABORT, 'synthetic failure'); END")
        try { backups.restoreWorkspace(original, true, true, master.toCharArray()); fail() } catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals(before, db.taskDao().getAll()); assertEquals(beforeVault, db.vaultDao().get())
        assertEquals(beforePrefs, prefs.snapshot()); assertNull(db.restoreJournalDao().get())
    }
    @Test fun clipboardCleanupDoesNotEraseAnotherApplicationsClip() {
        val context = RuntimeEnvironment.getApplication()
        val clipboard = VaultClipboard(context)
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.copy("synthetic secret")
        manager.setPrimaryClip(ClipData.newPlainText("Other app", "Keep this"))
        clipboard.clearOwned()
        assertEquals("Keep this", manager.primaryClip!!.getItemAt(0).text.toString())
        clipboard.copy("synthetic secret"); clipboard.clearOwned()
        assertFalse(manager.hasPrimaryClip())
    }
}
