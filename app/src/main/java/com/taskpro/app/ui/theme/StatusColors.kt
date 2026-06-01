package com.taskpro.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.taskpro.app.data.TaskStatus

/**
 * Colours for one task status:
 *  - [accent]  the strong status colour (stripe, icons)
 *  - [container] the soft card background
 *  - [onContainer] a readable text colour on top of [container]
 */
data class StatusPalette(val accent: Color, val container: Color, val onContainer: Color)

@Composable
@ReadOnlyComposable
fun statusPalette(status: TaskStatus): StatusPalette {
    // Follow the APP theme, not the system theme, so a light app on a dark
    // phone still gets light cards with dark text (and vice-versa).
    val dark = LocalAppDarkTheme.current
    return when (status) {
        TaskStatus.PENDING -> StatusPalette(
            accent = StatusPending,
            container = if (dark) PendingContainerDark else PendingContainer,
            onContainer = if (dark) OnContainerDark else OnContainerLight
        )
        TaskStatus.COMPLETED -> StatusPalette(
            accent = StatusCompleted,
            container = if (dark) CompletedContainerDark else CompletedContainer,
            onContainer = if (dark) OnContainerDark else OnContainerLight
        )
        TaskStatus.SNOOZED -> StatusPalette(
            accent = StatusSnoozed,
            container = if (dark) SnoozedContainerDark else SnoozedContainer,
            onContainer = if (dark) OnContainerDark else OnContainerLight
        )
    }
}
