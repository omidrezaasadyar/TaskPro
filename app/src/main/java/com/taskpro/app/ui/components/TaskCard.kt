package com.taskpro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.taskpro.app.data.Task
import com.taskpro.app.data.TaskStatus
import com.taskpro.app.ui.theme.statusPalette
import com.taskpro.app.util.DateFormat

/**
 * A single task row with a coloured status stripe and accent. Trailing
 * content (action buttons / drag handle) is supplied by the caller.
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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = palette.container),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Coloured status stripe
            Box(
                Modifier
                    .padding(start = 8.dp)
                    .width(5.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(palette.accent)
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (strike) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.notes.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
            }
            trailing()
        }
    }
}
