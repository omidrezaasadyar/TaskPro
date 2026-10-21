package com.taskpro.app.backup

import android.app.AlarmManager
import android.content.Context
import android.net.Uri
import androidx.room.Room
import com.taskpro.app.data.AppDatabase
import com.taskpro.app.data.Item
import com.taskpro.app.data.Task
import com.taskpro.app.data.TaskStatus
import com.taskpro.app.data.RestoreJournal
import com.taskpro.app.notification.ReminderScheduler
import com.taskpro.app.settings.BackgroundStyle
import com.taskpro.app.settings.SettingsRepository
import com.taskpro.app.settings.SettingsSnapshot
import com.taskpro.app.settings.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var backups: BackupRepository
    private lateinit var context: Context
    private val prefs = SettingsSnapshot(ThemeMode.DARK, BackgroundStyle.AURORA, 12345)

    @Before fun setup() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        settings = SettingsRepository(context)
        settings.replace(SettingsSnapshot(ThemeMode.SYSTEM, BackgroundStyle.GRADIENT, null))
        backups = BackupRepository(context, database, settings, ReminderScheduler(context))
    }

    @After fun close() { database.close() }

    private fun snapshot(): BackupSnapshot {
        val future = System.currentTimeMillis() + 86400000
        return BackupSnapshot(
            listOf(Item(50, "Group", -123, 42)),
            listOf(
                Task(100, 50, "Pending", "Notes", future, TaskStatus.PENDING, 3, 43),
                Task(101, 50, "Snoozed", "", future, TaskStatus.SNOOZED, 0, 44),
                Task(102, 50, "Done", "", future, TaskStatus.COMPLETED, 0, 45)
            ), prefs, 12345, "1.3.0"
        )
    }

    @Test fun replacesDataWithoutDuplicatesAndRearmsFutureReminders() = runBlocking {
        database.itemDao().insert(Item(1, "Old"))
        database.taskDao().insert(Task(1, 1, "Old task"))
        ReminderScheduler(context).schedule(1, "Old task", System.currentTimeMillis() + 60000)
        val expected = snapshot()
        backups.restore(expected)
        backups.restore(expected)
        assertEquals(expected.items, database.itemDao().getAll())
        assertEquals(expected.tasks, database.taskDao().getAll())
        assertEquals(prefs, settings.snapshot())
        assertNull(database.restoreJournalDao().get())
        val alarms = shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms
        assertEquals(2, alarms.size)
        assertTrue(database.taskDao().insert(Task(itemId = 50, title = "New")) > 102)
    }

    @Test fun databaseFailureRollsBackDeletionAndPreferencesStayUnchanged() = runBlocking {
        val old = Item(1, "Keep me")
        database.itemDao().insert(old)
        val previous = settings.snapshot()
        database.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER fail_import BEFORE INSERT ON tasks
            BEGIN SELECT RAISE(ABORT, 'simulated write failure'); END
        """.trimIndent())
        try {
            backups.restore(snapshot())
            fail("Restore should fail")
        } catch (expected: android.database.sqlite.SQLiteException) {
            assertEquals(listOf(old), database.itemDao().getAll())
            assertEquals(previous, settings.snapshot())
            assertNull(database.restoreJournalDao().get())
        }
    }

    @Test fun recoveryFinishesCommittedRestoreAndIsIdempotent() = runBlocking {
        val restored = snapshot()
        database.itemDao().insertAll(restored.items)
        database.taskDao().insertAll(restored.tasks)
        database.restoreJournalDao().insert(RestoreJournal(
            theme = prefs.theme.name, background = prefs.background.name,
            lastBackupAt = prefs.lastBackupAt, oldTaskIds = "[1,2]"
        ))
        backups.recover()
        backups.recover()
        assertEquals(prefs, settings.snapshot())
        assertEquals(restored.tasks, database.taskDao().getAll())
        assertNull(database.restoreJournalDao().get())
    }

    @Test fun exportedFileCanRestoreIntoEmptyDatabaseAndValidationDoesNotImport() = runBlocking {
        val original = snapshot()
        backups.restore(original)
        val file = File(context.cacheDir, "roundtrip.json")
        backups.create(Uri.fromFile(file))
        assertNotNull(settings.snapshot().lastBackupAt)
        database.itemDao().deleteAll()
        settings.replace(SettingsSnapshot(ThemeMode.LIGHT, BackgroundStyle.PLAIN, null))
        val selected = backups.read(Uri.fromFile(file))
        assertTrue(database.itemDao().getAll().isEmpty())
        backups.restore(selected)
        assertEquals(original.items, database.itemDao().getAll())
        assertEquals(original.tasks, database.taskDao().getAll())
        assertEquals(original.settings.theme, settings.snapshot().theme)
    }

    @Test fun versionOneDatabaseMigratesWithoutLosingExistingTasks() = runBlocking {
        val name = "migration-test.db"
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            old.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, colorArgb INTEGER, createdAt INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, itemId INTEGER NOT NULL, title TEXT NOT NULL, notes TEXT NOT NULL, dueAt INTEGER, status TEXT NOT NULL, position INTEGER NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(itemId) REFERENCES items(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            old.execSQL("CREATE INDEX index_tasks_itemId ON tasks (itemId)")
            old.execSQL("INSERT INTO items VALUES (1, 'Existing group', NULL, 42)")
            old.execSQL("INSERT INTO tasks VALUES (1, 1, 'Existing task', 'Keep notes', NULL, 'PENDING', 0, 43)")
            old.version = 1
        }
        val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build()
        try {
            assertEquals("Keep notes", upgraded.taskDao().getAll().single().notes)
            assertEquals("Existing group", upgraded.itemDao().getAll().single().name)
            assertNull(upgraded.restoreJournalDao().get())
        } finally { upgraded.close() }
    }
}
