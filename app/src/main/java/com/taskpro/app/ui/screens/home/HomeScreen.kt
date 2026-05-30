package com.taskpro.app.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskpro.app.R
import com.taskpro.app.data.Item
import com.taskpro.app.data.ItemWithTaskCounts
import com.taskpro.app.ui.AppViewModelProvider
import com.taskpro.app.ui.components.NameDialog
import com.taskpro.app.ui.navigation.Screen
import com.taskpro.app.ui.theme.StatusCompleted
import com.taskpro.app.ui.theme.StatusPending
import com.taskpro.app.ui.theme.StatusSnoozed
import com.taskpro.app.util.ShareUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onItemClick: (Long) -> Unit,
    onNavigate: (Screen) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Item?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Text(
                    stringResource(R.string.drawer_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Add, null) },
                    label = { Text(stringResource(R.string.add_new_item)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showAddDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.CheckCircle, null, tint = StatusCompleted) },
                    label = { Text(stringResource(R.string.completed)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate(Screen.Completed)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Snooze, null, tint = StatusSnoozed) },
                    label = { Text(stringResource(R.string.snoozed)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate(Screen.Snoozed)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text(stringResource(R.string.settings)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate(Screen.Settings)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, stringResource(R.string.menu))
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text(stringResource(R.string.add_new_item)) }
                )
            }
        ) { padding ->
            if (items.isEmpty()) {
                EmptyHome(Modifier.padding(padding))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.item.id }) { entry ->
                        ItemRow(
                            entry = entry,
                            onClick = { onItemClick(entry.item.id) },
                            onShare = {
                                scope.launch {
                                    val tasks = viewModel.exportData(entry.item.id)
                                    ShareUtil.share(context, entry.item.name, tasks)
                                }
                            },
                            onDelete = { viewModel.deleteItem(entry.item) },
                            onRename = { renameTarget = entry.item }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        NameDialog(
            title = stringResource(R.string.add_new_item),
            label = stringResource(R.string.item_name_label),
            confirmText = stringResource(R.string.add),
            onConfirm = {
                viewModel.addItem(it)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    renameTarget?.let { target ->
        NameDialog(
            title = stringResource(R.string.rename_item),
            label = stringResource(R.string.item_name_label),
            initialValue = target.name,
            confirmText = stringResource(R.string.save),
            onConfirm = {
                viewModel.renameItem(target, it)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }
}

@Composable
private fun ItemRow(
    entry: ItemWithTaskCounts,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.item.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send_a_copy),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CountChip(StatusPending, entry.pendingCount, stringResource(R.string.status_pending))
                CountChip(StatusSnoozed, entry.snoozedCount, stringResource(R.string.status_snoozed))
                CountChip(StatusCompleted, entry.completedCount, stringResource(R.string.status_completed))
            }
        }
    }
}

@Composable
private fun CountChip(color: androidx.compose.ui.graphics.Color, count: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text("$count $label", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun EmptyHome(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.empty_items_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.empty_items_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
