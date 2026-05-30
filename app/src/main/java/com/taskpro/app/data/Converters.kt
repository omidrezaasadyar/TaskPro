package com.taskpro.app.data

import androidx.room.TypeConverter

/** Room type converters for enums stored as their name. */
class Converters {
    @TypeConverter
    fun fromStatus(status: TaskStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): TaskStatus = TaskStatus.valueOf(value)
}
