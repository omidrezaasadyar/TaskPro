package com.taskpro.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Item::class, Task::class, RestoreJournal::class, com.taskpro.app.vault.VaultStore::class, com.taskpro.app.vault.VaultChunk::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun taskDao(): TaskDao
    abstract fun restoreJournalDao(): RestoreJournalDao
    abstract fun vaultDao(): com.taskpro.app.vault.VaultDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS vault_state (id INTEGER NOT NULL PRIMARY KEY, envelope TEXT NOT NULL, revision INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS vault_chunks (position INTEGER NOT NULL PRIMARY KEY, ciphertext TEXT NOT NULL)")
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS restore_journal (
                        id INTEGER NOT NULL PRIMARY KEY,
                        theme TEXT NOT NULL,
                        background TEXT NOT NULL,
                        lastBackupAt INTEGER,
                        oldTaskIds TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taskpro.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}
