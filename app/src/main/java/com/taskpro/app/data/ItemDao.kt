package com.taskpro.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query(
        """
        SELECT i.*,
            (SELECT COUNT(*) FROM tasks t WHERE t.itemId = i.id AND t.status = 'PENDING')   AS pendingCount,
            (SELECT COUNT(*) FROM tasks t WHERE t.itemId = i.id AND t.status = 'COMPLETED') AS completedCount,
            (SELECT COUNT(*) FROM tasks t WHERE t.itemId = i.id AND t.status = 'SNOOZED')   AS snoozedCount
        FROM items i
        ORDER BY i.createdAt DESC
        """
    )
    fun observeItemsWithCounts(): Flow<List<ItemWithTaskCounts>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun observeItem(id: Long): Flow<Item?>

    @Insert
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)
}
