package com.taskpro.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** The lifecycle state of a task, each rendered with its own colour. */
enum class TaskStatus {
    /** Still to be done — shown in red. */
    PENDING,
    /** Completed — shown in green. */
    COMPLETED,
    /** Snoozed for later — shown in orange. */
    SNOOZED
}

/**
 * A single actionable task belonging to an [Item].
 *
 * [position] drives manual priority ordering (drag to reorder); lower values
 * appear first. [dueAt] is the epoch-millis reminder time, null when no
 * reminder is set.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId")]
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val title: String,
    val notes: String = "",
    val dueAt: Long? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
