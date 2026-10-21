package com.taskpro.app.ui.screens.item

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskpro.app.R
import com.taskpro.app.data.*
import com.taskpro.app.ui.AppViewModelProvider
import com.taskpro.app.ui.components.*
import com.taskpro.app.ui.components.shared.IconAction
import com.taskpro.app.ui.screens.tasks.ManagedTaskRow
import com.taskpro.app.ui.screens.tasks.taskMetadata
import com.taskpro.app.ui.theme.LocalVaurivoColors
import com.taskpro.app.util.ShareUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemDetailScreen(onBack: () -> Unit, onOpenStatusList: (Long, String) -> Unit,
    viewModel: ItemDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val item by viewModel.item.collectAsStateWithLifecycle()
    val tasks by viewModel.pendingTasks.collectAsStateWithLifecycle()
    val completed by viewModel.completedCount.collectAsStateWithLifecycle()
    val snoozed by viewModel.snoozedCount.collectAsStateWithLifecycle()
    val p = LocalVaurivoColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var suppressEditUntil by remember { mutableLongStateOf(0L) }
    val dragHint = stringResource(R.string.reorder_hint)
    fun action(block: suspend () -> Unit) {
        if (!busy) { busy = true; scope.launch {
            try { block() } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { failed = true } finally { busy = false }
        } }
    }
    val localTasks = remember { mutableStateListOf<Task>() }
    val listState = rememberLazyListState()
    val drag = rememberDragDropState(listState, onMove = { from, to ->
        if (!busy && from in localTasks.indices && to in localTasks.indices) localTasks.add(to, localTasks.removeAt(from))
    }, onDragEnd = {
        suppressEditUntil = android.os.SystemClock.uptimeMillis() + 250
        val ordered = localTasks.toList()
        action { viewModel.reorder(ordered) }
    })
    LaunchedEffect(tasks, drag.draggingItemIndex, busy) {
        if (drag.draggingItemIndex == null && !busy) { localTasks.clear(); localTasks.addAll(tasks) }
    }
    fun move(from: Int, to: Int) {
        if (from in localTasks.indices && to in localTasks.indices) {
            localTasks.add(to, localTasks.removeAt(from))
            val ordered = localTasks.toList()
            action { viewModel.reorder(ordered) }
        }
    }
    Scaffold(containerColor = p.canvas, topBar = {
        TopAppBar(title = { Text(item?.name.orEmpty()) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = p.canvas),
            navigationIcon = { IconAction(stringResource(R.string.back), Icons.AutoMirrored.Filled.ArrowBack, onBack) },
            actions = { IconAction(stringResource(R.string.send_a_copy), Icons.AutoMirrored.Filled.Send, {
                action { ShareUtil.share(context, item?.name.orEmpty(), viewModel.tasksForExport()) }
            }) })
    }, bottomBar = {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.End) {
        com.taskpro.app.ui.components.shared.PrimaryActionButton(stringResource(R.string.tasks_new), { creating = true },
            icon = Icons.Default.Add, compact = true)
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onOpenStatusList(viewModel.itemId, TaskStatus.COMPLETED.name) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("${stringResource(R.string.completed)} · $completed")
                }
                OutlinedButton(onClick = { onOpenStatusList(viewModel.itemId, TaskStatus.SNOOZED.name) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("${stringResource(R.string.snoozed)} · $snoozed")
                }
            }
            // Outside the LazyColumn: drag indexes now match task indexes exactly.
            if (localTasks.isEmpty()) Text(stringResource(R.string.no_tasks_yet), Modifier.padding(vertical = 24.dp), color = p.textSecondary)
            else LazyColumn(state = listState, modifier = Modifier.weight(1f).dragContainer(drag), contentPadding = PaddingValues(bottom = 16.dp)) {
                itemsIndexed(localTasks, key = { _, task -> task.id }) { index, task ->
                    DraggableItem(drag, index) {
                        ManagedTaskRow(task, taskMetadata(item?.name.orEmpty(), task.dueAt), onEdit = {
                            if (drag.draggingItemIndex == null && android.os.SystemClock.uptimeMillis() >= suppressEditUntil) editing = task
                        }, onStatus = { status -> action { viewModel.changeStatus(task, status) } },
                            onDelete = { action { viewModel.remove(task) } }, busy = busy, dragging = drag.draggingItemIndex != null,
                            onMoveUp = if (index > 0) ({ move(index, index - 1) }) else null,
                            onMoveDown = if (index < localTasks.lastIndex) ({ move(index, index + 1) }) else null,
                            dragHandle = { Box(Modifier.size(48.dp).semantics { stateDescription = dragHint }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DragHandle, stringResource(R.string.drag_to_reorder), tint = p.textTertiary)
                            } })
                    }
                }
            }
        }
    }
    if (creating || editing != null) AddTaskSheet(taskToEdit = editing, onDismiss = { creating = false; editing = null },
        onSave = { _, title, notes, due -> viewModel.save(editing, title, notes, due) })
    if (failed) AlertDialog(onDismissRequest = { failed = false }, text = { Text(stringResource(R.string.tasks_action_failed)) },
        confirmButton = { TextButton(onClick = { failed = false }) { Text(stringResource(R.string.ok)) } })
}
