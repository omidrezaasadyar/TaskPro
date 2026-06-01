package com.taskpro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.taskpro.app.data.Task
import com.taskpro.app.data.TaskStatus
import com.taskpro.app.ui.theme.LocalAppDarkTheme
import com.taskpro.app.ui.theme.NoteTextDark
import com.taskpro.app.ui.theme.NoteTextLight
import com.taskpro.app.ui.theme.statusPalette
import com.taskpro.app.util.DateFormat
import com.taskpro.app.util.isRtlText

/**
 * A single task row.
 *
 * Layout: a coloured status stripe on the left, then the task content. The
 * title (and any note) occupy a full-width line and align right for Persian /
 * left for Latin text. The action buttons supplied via [trailing] sit on their
 * own row below the text so long titles never collide with the controls.
 */
@Composable
fun TaskCard(
    task: Task,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    trailing: @Composable () -> Unit = {}
) {
    val palette = statusPalette(task.status)
    val strike = task.status == TaskStatus.COMPLETED
    val noteColor = if (LocalAppDarkTheme.current) NoteTextDark else NoteTextLight

    val titleAlign = if (isRtlText(task.title)) TextAlign.Right else TextAlign.Left
    val noteAlign = if (isRtlText(task.notes)) TextAlign.Right else TextAlign.Left

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = palette.container),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 1.dp)
    ) {
        Row(
            // IntrinsicSize.Min lets the stripe stretch to the card's height.
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                Modifier
                    .padding(start = 8.dp, top = 10.dp, bottom = 10.dp)
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(palette.accent)
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Title — full width, smaller, not bold, direction-aware.
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.onContainer,
                    textAlign = titleAlign,
                    textDecoration = if (strike) TextDecoration.LineThrough else null,
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional note — distinct colour, direction-aware.
                if (task.notes.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = noteColor,
                        textAlign = noteAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Optional reminder time.
                task.dueAt?.let { due ->
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = DateFormat.formatDateTime(due),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accent
                        )
                    }
                }

                // Action buttons on their own row, below the text.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    trailing()
                }
            }
        }
    }
}
