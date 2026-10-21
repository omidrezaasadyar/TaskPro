package com.taskpro.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/** Committed with restored rows; replayed until preferences and OS alarms are reconciled. */
@Entity(tableName = "restore_journal")
data class RestoreJournal(
    @PrimaryKey val id: Int = 1,
    val theme: String,
    val background: String,
    val lastBackupAt: Long?,
    val oldTaskIds: String
)

@Dao
interface RestoreJournalDao {
    @Query("SELECT * FROM restore_journal WHERE id = 1")
    suspend fun get(): RestoreJournal?

    @Insert
    suspend fun insert(journal: RestoreJournal)

    @Query("DELETE FROM restore_journal")
    suspend fun clear()
}
