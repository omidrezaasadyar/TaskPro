package com.taskpro.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskpro.app.R
import com.taskpro.app.data.Item
import com.taskpro.app.data.ItemWithTaskCounts
import com.taskpro.app.ui.AppViewModelProvider
import com.taskpro.app.ui.components.GradientBackground
import com.taskpro.app.ui.components.NameDialog
import com.taskpro.app.ui.navigation.Screen
import com.taskpro.app.ui.theme.StatusCompleted
import com.taskpro.app.ui.theme.StatusPending
import com.taskpro.app.ui.theme.StatusSnoozed
import com.taskpro.app.ui.theme.itemAccentColor
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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Item?>(null) }
    var deleteTarget by remember { mutableStateOf<Item?>(null) }

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
                    icon = { Icon(Icons.Default.Search, null) },
                    label = { Text(stringResource(R.string.search)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate(Screen.Search)
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
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, stringResource(R.string.menu))
                        }
                    },
                    actions = {
                        IconButton(onClick = { onNavigate(Screen.Search) }) {
                            Icon(Icons.Default.Search, stringResource(R.string.search))
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
            GradientBackground {
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
                            onDelete = { deleteTarget = entry.item }
                        )
                    }
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

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = { Icon(Icons.Default.DeleteOutline, null) },
            title = { Text(stringResource(R.string.delete_item_title)) },
            text = { Text(stringResource(R.string.delete_item_message, target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteItem(target)
                    deleteTarget = null
                }) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemRow(
    entry: ItemWithTaskCounts,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = itemAccentColor(entry.item.id, entry.item.colorArgb)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth()) {
            // Coloured identity stripe down the left side of the item.
            Box(
                Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = entry.item.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                Spacer(Modifier.height(10.dp))
                // Status counters: number on top, small label below, all on one row.
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CountCell(
                        StatusPending,
                        entry.pendingCount,
                        stringResource(R.string.status_pending),
                        Modifier.weight(1f)
                    )
                    CountCell(
                        StatusSnoozed,
                        entry.snoozedCount,
                        stringResource(R.string.status_snoozed),
                        Modifier.weight(1f)
                    )
                    CountCell(
                        StatusCompleted,
                        entry.completedCount,
                        stringResource(R.string.status_completed),
                        Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** A vertical counter: the number on top, a small coloured label underneath. */
@Composable
private fun CountCell(color: Color, count: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyHome(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.illustration_empty_tasks),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(200.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.empty_items_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.empty_items_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
