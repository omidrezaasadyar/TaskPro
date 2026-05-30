package com.taskpro.app.data

import androidx.room.Embedded

/** A [Task] joined with the name of its parent item, used for search results. */
data class TaskWithItemName(
    @Embedded val task: Task,
    val itemName: String
)
