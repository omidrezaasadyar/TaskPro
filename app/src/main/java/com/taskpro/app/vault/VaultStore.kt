package com.taskpro.app.vault

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction

@Entity(tableName = "vault_state")
data class VaultStore(@PrimaryKey val id: Int = 1, val envelope: String, val revision: Long = 0)

@Entity(tableName = "vault_chunks")
data class VaultChunk(@PrimaryKey val position: Int, val ciphertext: String)

@Dao
abstract class VaultDao {
    @Query("SELECT * FROM vault_state WHERE id = 1")
    protected abstract suspend fun header(): VaultStore?
    @Query("SELECT * FROM vault_chunks ORDER BY position")
    protected abstract suspend fun chunks(): List<VaultChunk>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertHeader(store: VaultStore)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertChunks(chunks: List<VaultChunk>)
    @Query("DELETE FROM vault_chunks")
    protected abstract suspend fun clearChunks()
    @Query("UPDATE vault_state SET envelope = :envelope, revision = revision + 1 WHERE id = 1 AND revision = :revision")
    protected abstract suspend fun updateHeader(envelope: String, revision: Long): Int

    @Transaction
    open suspend fun get(): VaultStore? {
        val stored = header() ?: return null
        return stored.copy(envelope = stored.envelope + chunks().joinToString("") { it.ciphertext })
    }

    @Transaction
    open suspend fun insert(store: VaultStore) {
        val parts = store.envelope.chunked(256 * 1024)
        insertHeader(store.copy(envelope = parts.first()))
        clearChunks()
        insertChunks(parts.drop(1).mapIndexed { i, part -> VaultChunk(i, part) })
    }

    @Transaction
    open suspend fun update(envelope: String, revision: Long): Int {
        // Bound each row well below Android's CursorWindow limit, preserving one atomic revision.
        val parts = envelope.chunked(256 * 1024)
        val changed = updateHeader(parts.first(), revision)
        if (changed == 1) {
            clearChunks()
            insertChunks(parts.drop(1).mapIndexed { i, part -> VaultChunk(i, part) })
        }
        return changed
    }
}
