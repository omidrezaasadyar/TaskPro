package com.taskpro.app.ui.screens.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskpro.app.R
import com.taskpro.app.data.Task
import com.taskpro.app.data.TaskStatus
import com.taskpro.app.ui.AppViewModelProvider
import com.taskpro.app.ui.components.DraggableItem
import com.taskpro.app.ui.components.GradientBackground
import com.taskpro.app.ui.components.TaskCard
import com.taskpro.app.ui.components.dragContainer
import com.taskpro.app.ui.components.rememberDragDropState
import com.taskpro.app.ui.theme.StatusCompleted
import com.taskpro.app.ui.theme.StatusSnoozed
import com.taskpro.app.util.ShareUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    onBack: () -> Unit,
    onOpenStatusList: (itemId: Long, status: String) -> Unit,
    viewModel: ItemDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val item by viewModel.item.collectAsStateWithLifecycle()
    val tasks by viewModel.pendingTasks.collectAsStateWithLifecycle()
    val completedCount by viewModel.completedCount.collectAsStateWithLifecycle()
    val snoozedCount by viewModel.snoozedCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAddSheet by remember { mutableStateOf(false) }

    // Local mutable copy so drag reordering feels instant; resynced from the
    // database whenever we are not mid-drag.
    val localTasks: SnapshotStateList<Task> = remember { mutableStateListOf() }
    val listState = rememberLazyListState()

    val dragState = rememberDragDropState(
        listState = listState,
        onMove = { from, to ->
            if (from in localTasks.indices && to in localTasks.indices) {
                localTasks.add(to, localTasks.removeAt(from))
            }
        },
        onDragEnd = { viewModel.persistOrder(localTasks.toList()) }
    )

    LaunchedEffect(tasks) {
        if (dragState.draggingItemIndex == null) {
            localTasks.clear()
            localTasks.addAll(tasks)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(item?.name ?: stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val all = viewModel.tasksForExport()
                            ShareUtil.share(context, item?.name ?: "", all)
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            stringResource(R.string.send_a_copy)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, stringResource(R.string.add_task))
            }
        }
    ) { padding ->
        GradientBackground {
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Completed / Snoozed summary boxes — each on its own full-width row
            // so the labels never wrap. Always visible and tappable.
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusSummaryBox(
                    label = stringResource(R.string.completed),
                    count = completedCount,
                    accent = StatusCompleted,
                    icon = Icons.Default.CheckCircle,
                    onClick = {
                        onOpenStatusList(viewModel.itemId, TaskStatus.COMPLETED.name)
                    }
                )
                StatusSummaryBox(
                    label = stringResource(R.string.snoozed),
                    count = snoozedCount,
                    accent = StatusSnoozed,
                    icon = Icons.Default.Snooze,
                    onClick = {
                        onOpenStatusList(viewModel.itemId, TaskStatus.SNOOZED.name)
                    }
                )
            }

            if (localTasks.isEmpty()) {
                EmptyTasks(Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .dragContainer(dragState),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            stringResource(R.string.reorder_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    itemsIndexed(localTasks, key = { _, t -> t.id }) { index, task ->
                    DraggableItem(dragDropState = dragState, index = index) { isDragging ->
                        TaskCard(task = task, isDragging = isDragging) {
                            Row {
                                IconButton(onClick = { viewModel.complete(task) }) {
                                    Icon(
                                        Icons.Default.Check,
                                        stringResource(R.string.mark_complete),
                                        tint = StatusCompleted
                                    )
                                }
                                IconButton(onClick = { viewModel.snooze(task) }) {
                                    Icon(
                                        Icons.Default.Snooze,
                                        stringResource(R.string.snooze),
                                        tint = StatusSnoozed
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteTask(task) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = stringResource(R.string.drag_to_reorder),
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .padding(end = 8.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
        }
    }

    if (showAddSheet) {
        AddTaskSheet(
            onDismiss = { showAddSheet = false },
            onConfirm = { title, notes, dueAt ->
                viewModel.addTask(title, notes, dueAt)
                showAddSheet = false
            }
        )
    }
}

/**
 * A tappable box summarising how many of this item's tasks are Completed or
 * Snoozed. Tapping it opens the full list for that status.
 */
@Composable
private fun StatusSummaryBox(
    label: String,
    count: Int,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        // Tinted with the status accent so it reads in both light and dark themes.
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
            Spacer(Modifier.size(12.dp))
            // Label on a single line; it has the whole row width so it never wraps.
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            // Count badge
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accent)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.size(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accent
            )
        }
    }
}

@Composable
private fun EmptyTasks(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.illustration_empty_tasks),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(180.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.no_tasks_yet),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.add_first_task),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
