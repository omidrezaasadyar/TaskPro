package com.taskpro.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lightweight date/time formatting helpers for reminders. */
object DateFormat {

    private val dateTime = SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault())
    private val timeOnly = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateOnly = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    fun formatDateTime(millis: Long): String = dateTime.format(Date(millis))
    fun formatTime(millis: Long): String = timeOnly.format(Date(millis))
    fun formatDate(millis: Long): String = dateOnly.format(Date(millis))
}
