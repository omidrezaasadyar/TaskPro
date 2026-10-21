package com.taskpro.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskpro.app.R
import com.taskpro.app.data.*
import com.taskpro.app.ui.AppViewModelProvider
import com.taskpro.app.ui.components.NameDialog
import com.taskpro.app.ui.components.shared.IconAction
import com.taskpro.app.ui.navigation.Screen
import com.taskpro.app.ui.screens.tasks.TasksViewSelector
import com.taskpro.app.ui.theme.LocalVaurivoColors
import com.taskpro.app.util.ShareUtil
import kotlinx.coroutines.launch

/** The existing home route remains the group destination; Timeline has its own route. */
@Composable
fun HomeScreen(onItemClick: (Long) -> Unit, onNavigate: (Screen) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<Item?>(null) }
    var delete by remember { mutableStateOf<Item?>(null) }
    var error by remember { mutableStateOf(false) }
    GroupsScreen(items, onItemClick, { adding = true }, { rename = it }, { delete = it },
        onShare = { item -> scope.launch {
            try { ShareUtil.share(context, item.name, viewModel.exportData(item.id)) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { error = true }
        } }, onTimeline = { onNavigate(Screen.Tasks) },
        onSearch = { onNavigate(Screen.Search) }, onSettings = { onNavigate(Screen.Settings) })
    if (adding) NameDialog(stringResource(R.string.tasks_new_group), stringResource(R.string.tasks_group_name),
        stringResource(R.string.add), { viewModel.addItem(it); adding = false }, { adding = false })
    rename?.let { item -> NameDialog(stringResource(R.string.tasks_rename_group), stringResource(R.string.tasks_group_name),
        stringResource(R.string.save), { viewModel.renameItem(item, it); rename = null }, { rename = null }, initialValue = item.name) }
    delete?.let { item -> AlertDialog(onDismissRequest = { delete = null },
        title = { Text(stringResource(R.string.delete_item_title)) },
        text = { Text(stringResource(R.string.delete_item_message, item.name)) },
        confirmButton = { TextButton(onClick = { viewModel.deleteItem(item); delete = null }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { delete = null }) { Text(stringResource(R.string.cancel)) } }) }
    if (error) AlertDialog(onDismissRequest = { error = false }, text = { Text(stringResource(R.string.tasks_action_failed)) },
        confirmButton = { TextButton(onClick = { error = false }) { Text(stringResource(R.string.ok)) } })
}

@Composable
fun GroupsScreen(items: List<ItemWithTaskCounts>, onItemClick: (Long) -> Unit, onAdd: () -> Unit,
    onRename: (Item) -> Unit, onDelete: (Item) -> Unit, onShare: (Item) -> Unit,
    onTimeline: () -> Unit, onSearch: () -> Unit, onSettings: () -> Unit) {
    val p = LocalVaurivoColors.current
    Scaffold(containerColor = p.canvas, bottomBar = {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.End) {
        com.taskpro.app.ui.components.shared.PrimaryActionButton(stringResource(R.string.tasks_new_group), onAdd,
            icon = Icons.Default.Add, compact = true)
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.tasks_title), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, color = p.textPrimary)
                    IconAction(stringResource(R.string.search), Icons.Default.Search, onSearch)
                    IconAction(stringResource(R.string.settings), Icons.Default.Settings, onSettings)
                }
                TasksViewSelector(true, onTimeline, {})
            }
            if (items.isEmpty()) item {
                Column(Modifier.padding(vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FolderOpen, null, tint = p.tasksAccent, modifier = Modifier.size(32.dp))
                    Text(stringResource(R.string.empty_items_title), style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
                    Text(stringResource(R.string.empty_items_subtitle), style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
                }
            }
            items(items, key = { it.item.id }) { entry ->
                var menu by remember(entry.item.id) { mutableStateOf(false) }
                Surface(onClick = { onItemClick(entry.item.id) }, color = p.surface, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, null, tint = entry.item.colorArgb?.let { androidx.compose.ui.graphics.Color(it) } ?: p.tasksAccent, modifier = Modifier.size(32.dp))
                            Text(entry.item.name, Modifier.weight(1f).padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content), color = p.textPrimary)
                            Box {
                                IconAction(stringResource(R.string.component_menu_for, entry.item.name), Icons.Default.MoreVert, { menu = true })
                                DropdownMenu(menu, { menu = false }) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.tasks_rename_group)) }, onClick = { menu = false; onRename(entry.item) })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.send_a_copy)) }, onClick = { menu = false; onShare(entry.item) })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { menu = false; onDelete(entry.item) })
                                }
                            }
                        }
                        Text(stringResource(R.string.tasks_counts, entry.pendingCount, entry.completedCount, entry.snoozedCount),
                            style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                    }
                }
            }
        }
    }
}
