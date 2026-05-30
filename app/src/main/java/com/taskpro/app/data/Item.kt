package com.taskpro.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A top-level "item" the user manages tasks for — e.g. an Omani company,
 * an English company, an Iranian company, or themselves.
 */
@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Optional colour accent (ARGB int) chosen by the user for this item. */
    val colorArgb: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
