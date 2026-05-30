package com.taskpro.app.data

import androidx.room.Embedded

/** An [Item] enriched with aggregate task counts for the home list summary. */
data class ItemWithTaskCounts(
    @Embedded val item: Item,
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val snoozedCount: Int = 0
)
