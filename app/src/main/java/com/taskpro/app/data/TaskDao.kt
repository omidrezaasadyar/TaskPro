package com.taskpro.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    /** All tasks for an item in a given status, ordered by manual priority. */
    @Query("SELECT * FROM tasks WHERE itemId = :itemId AND status = :status ORDER BY position ASC")
    fun observeTasks(itemId: Long, status: TaskStatus): Flow<List<Task>>

    /** Every task in a status across all items (for the global Completed/Snoozed screens). */
    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY position ASC")
    fun observeAllByStatus(status: TaskStatus): Flow<List<Task>>

    /** Search tasks by title or notes (case-insensitive), tagged with their item name. */
    @Query(
        """
        SELECT tasks.*, items.name AS itemName
        FROM tasks
        JOIN items ON tasks.itemId = items.id
        WHERE tasks.title LIKE '%' || :query || '%'
           OR tasks.notes LIKE '%' || :query || '%'
        ORDER BY tasks.status ASC, tasks.position ASC
        """
    )
    fun searchTasks(query: String): Flow<List<TaskWithItemName>>

    @Query("SELECT * FROM tasks WHERE itemId = :itemId ORDER BY status ASC, position ASC")
    suspend fun getTasksForItem(itemId: Long): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    /** All tasks that still have a future reminder — used to re-arm alarms on boot. */
    @Query("SELECT * FROM tasks WHERE dueAt IS NOT NULL AND status = 'PENDING'")
    suspend fun getTasksWithReminders(): List<Task>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM tasks WHERE itemId = :itemId AND status = :status")
    suspend fun nextPosition(itemId: Long, status: TaskStatus): Int

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Update
    suspend fun updateAll(tasks: List<Task>)

    @Delete
    suspend fun delete(task: Task)

    @Transaction
    suspend fun reorder(tasks: List<Task>) {
        updateAll(tasks.mapIndexed { index, task -> task.copy(position = index) })
    }
}
