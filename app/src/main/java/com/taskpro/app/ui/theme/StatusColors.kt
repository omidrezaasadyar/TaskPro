package com.taskpro.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.taskpro.app.data.TaskStatus

/** Accent + container colour pair for a task status, dark-mode aware. */
data class StatusPalette(val accent: Color, val container: Color)

@Composable
fun statusPalette(status: TaskStatus): StatusPalette {
    val dark = isSystemInDarkTheme()
    return when (status) {
        TaskStatus.PENDING -> StatusPalette(
            StatusPending, if (dark) PendingContainerDark else PendingContainer
        )
        TaskStatus.COMPLETED -> StatusPalette(
            StatusCompleted, if (dark) CompletedContainerDark else CompletedContainer
        )
        TaskStatus.SNOOZED -> StatusPalette(
            StatusSnoozed, if (dark) SnoozedContainerDark else SnoozedContainer
        )
    }
}
