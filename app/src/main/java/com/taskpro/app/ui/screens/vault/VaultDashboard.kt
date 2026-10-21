package com.taskpro.app.ui.screens.vault

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.SecureFlagPolicy
import com.taskpro.app.R
import com.taskpro.app.ui.components.shared.*
import com.taskpro.app.ui.theme.*
import com.taskpro.app.vault.*

/** Authenticated, session-local presentation. The owner validates callbacks against its live session. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VaultDashboard(doc: VaultDocument, folders: List<VaultCategory>, records: List<VaultRecord>,
    query: String, onQuery: (String) -> Unit, tag: String, onTag: (String) -> Unit,
    favorites: Boolean, onFavorites: (Boolean) -> Unit, sort: Int, onSort: (Int) -> Unit,
    category: String?, allRecords: Boolean, onAll: () -> Unit, onParent: () -> Unit,
    onFolder: (String) -> Unit, onCategories: () -> Unit, onNewGroup: () -> Unit,
    onEditGroup: (VaultCategory) -> Unit, onDeleteGroup: (VaultCategory) -> Unit,
    onReorder: (VaultCategory, Int) -> Unit, onRecord: (String) -> Unit, onFavorite: (String) -> Unit,
    onNewRecord: () -> Unit, onLock: () -> Unit, onSecurity: () -> Unit,
    onSettings: () -> Unit, onWorkspace: () -> Unit, modifier: Modifier = Modifier, searchModifier: Modifier = Modifier,
    searchRequest: Int = 0, onSearchReady: () -> Unit = {},
    busy: Boolean = false) {
    val p = LocalVaurivoColors.current
    var menu by remember { mutableStateOf(false) }
    var filters by remember { mutableStateOf(false) }
    var groupMenu by remember { mutableStateOf<String?>(null) }
    val overview = category == null && !allRecords && query.isBlank() && tag.isBlank() && !favorites && sort == 0
    val preview = if (overview) records.filter { it.favorite }.take(3) else emptyList()
    val remaining = records.filterNot { record -> preview.any { it.id == record.id } }
    val activeFilters = favorites || tag.isNotBlank() || sort != 0
    val filterLabel = queryScopeLabel(tag, favorites, sort)
    val overviewTitle = stringResource(R.string.vault_overview)
    val listState = rememberLazyListState()
    LaunchedEffect(searchRequest) {
        if (searchRequest > 0) { listState.scrollToItem(1); withFrameNanos { }; onSearchReady() }
    }
    Scaffold(containerColor = p.canvas, contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
            TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text(stringResource(R.string.vault_new_record)) } }, state = rememberTooltipState()) {
                PrimaryActionButton(stringResource(R.string.vault_add_item), onNewRecord, module = ModuleAccent.Vault,
                    icon = Icons.Default.Add, compact = true, enabled = !busy)
            }
        }
    }) { padding ->
        BoxWithConstraints(modifier.fillMaxSize().padding(padding)) {
            val columns = if (maxWidth >= 356.dp && LocalDensity.current.fontScale <= 1.3f) 2 else 1
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.vault_title), Modifier.weight(1f).semantics { heading() },
                            style = MaterialTheme.typography.headlineSmall, color = p.textPrimary)
                        Box {
                            IconAction(stringResource(R.string.vault_filters), Icons.Default.SwapVert, { filters = true }, module = ModuleAccent.Vault,
                                modifier = Modifier.semantics { if (activeFilters) stateDescription = filterLabel })
                            if (activeFilters) Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(6.dp).background(p.vaultAccent, VaurivoShapes.control))
                        }
                        IconAction(stringResource(R.string.vault_lock), Icons.Default.Lock, onLock, module = ModuleAccent.Vault)
                        Box {
                            IconAction(stringResource(R.string.menu), Icons.Default.MoreVert, { menu = true }, module = ModuleAccent.Vault)
                            DropdownMenu(menu, { menu = false }) {
                                listOf(R.string.vault_categories to onCategories, R.string.vault_security to onSecurity,
                                    R.string.settings to onSettings, R.string.workspace_switch to onWorkspace).forEach { (label, action) ->
                                    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { menu = false; action() })
                                }
                            }
                        }
                    }
                }
                item { DashboardSearch(query, onQuery, searchModifier) }
                if (activeFilters) item { Text(queryScopeLabel(tag, favorites, sort), style = MaterialTheme.typography.bodySmall, color = p.textSecondary) }
                if (category != null || allRecords || query.isNotBlank()) item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconAction(stringResource(R.string.back), Icons.Default.ArrowUpward, onParent, module = ModuleAccent.Vault)
                        Text(when {
                            query.isNotBlank() -> stringResource(R.string.vault_search_all)
                            category == "" -> stringResource(R.string.vault_uncategorized)
                            category == null -> stringResource(R.string.vault_all)
                            else -> categoryPath(category, doc.categories)
                        }, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleSmall, color = p.textSecondary)
                    }
                }
                item {
                    Column(Modifier.semantics { paneTitle = overviewTitle }) {
                        SectionHeader(stringResource(R.string.vault_groups_heading), actionLabel = stringResource(R.string.vault_new_group_label),
                            onAction = onNewGroup, enabled = !busy, module = ModuleAccent.Vault)
                        val accessibleCounts = "${doc.categories.size} ${stringResource(R.string.vault_total_groups)}, ${doc.records.size} ${stringResource(R.string.vault_total_items)}"
                        Text("${pluralStringResource(R.plurals.vault_group_total, doc.categories.size, doc.categories.size)} · ${pluralStringResource(R.plurals.vault_item_total, doc.records.size, doc.records.size)}",
                            Modifier.semantics { contentDescription = accessibleCounts },
                            style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                    }
                }
                items(folders.chunked(columns), key = { row -> "folders:${row.first().id}" }) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { folder ->
                            Box(Modifier.weight(1f)) {
                                VaultGroupCard(folder, true, { onFolder(folder.id) }, { groupMenu = folder.id },
                                    directItems = doc.records.count { it.categoryId == folder.id },
                                    subgroups = doc.categories.count { it.parentId == folder.id }, compact = true, enabled = !busy)
                                DropdownMenu(groupMenu == folder.id, { groupMenu = null }) {
                                    listOf(R.string.vault_edit to { onEditGroup(folder) },
                                        R.string.vault_move_up to { onReorder(folder, -1) },
                                        R.string.vault_move_down to { onReorder(folder, 1) },
                                        R.string.delete to { onDeleteGroup(folder) }).forEach { (label, action) ->
                                        DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { groupMenu = null; action() })
                                    }
                                }
                            }
                        }
                        if (row.size < columns) Spacer(Modifier.weight(1f))
                    }
                }
                if (preview.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.vault_favorites_heading),
                        actionLabel = if (records.count { it.favorite } > preview.size) stringResource(R.string.vault_view_all) else null,
                        onAction = { onFavorites(true) }, module = ModuleAccent.Vault) }
                    items(preview, key = { "favorite:${it.id}" }) { record ->
                        DashboardRecord(record, doc, onRecord, onFavorite, busy)
                    }
                }
                item { SectionHeader(stringResource(if (favorites) R.string.vault_favorites_heading else R.string.vault_items),
                    actionLabel = stringResource(R.string.vault_all), onAction = onAll, module = ModuleAccent.Vault) }
                if (remaining.isEmpty() && preview.isEmpty()) item {
                    val unfilteredEmpty = query.isBlank() && !activeFilters
                    EmptyState(stringResource(if (unfilteredEmpty) R.string.vault_empty else R.string.vault_no_results),
                        stringResource(if (unfilteredEmpty) R.string.vault_empty_hint else R.string.vault_browse_hint),
                        Icons.Default.FolderOpen, module = ModuleAccent.Vault)
                }
                items(remaining, key = { it.id }) { record -> DashboardRecord(record, doc, onRecord, onFavorite, busy) }
            }
        }
    }
    if (filters) {
        val title = stringResource(R.string.vault_filters)
        val height = (LocalConfiguration.current.screenHeightDp * .85f).dp
        Dialog(onDismissRequest = { filters = false }, properties = DialogProperties(usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)) {
            Surface(Modifier.padding(16.dp).widthIn(max = 560.dp).fillMaxWidth().heightIn(max = height).imePadding().semantics { paneTitle = title },
                color = p.surfaceRaised, shape = VaurivoShapes.card) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                VaultChoice(stringResource(R.string.vault_sort), sort, listOf(0 to stringResource(R.string.vault_sort_name),
                    1 to stringResource(R.string.vault_sort_modified), 2 to stringResource(R.string.vault_sort_created),
                    3 to stringResource(R.string.vault_sort_category)), onSort)
                VaultToggle(stringResource(R.string.vault_favorites), favorites, onFavorites)
                VaultInput(stringResource(R.string.vault_tag_filter), tag, onTag)
                TextButton(onClick = { onSort(0); onTag(""); onFavorites(false) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.vault_reset_filters)) }
                TextButton(onClick = { filters = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.ok)) }
            }
            }
        }
    }
}

@Composable
private fun DashboardSearch(query: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalVaurivoColors.current
    val label = stringResource(R.string.search)
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    Surface(shape = VaurivoShapes.control, color = p.surface,
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) p.vaultAccent else p.outlineInteractive)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Search, null, Modifier.size(20.dp), tint = p.textSecondary)
            BasicTextField(query, onQuery, modifier.weight(1f).heightIn(min = 48.dp).semantics { contentDescription = label },
                singleLine = true, interactionSource = interactions, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = p.textPrimary, textDirection = TextDirection.Content),
                cursorBrush = SolidColor(p.vaultAccent), decorationBox = { inner ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text(label, style = MaterialTheme.typography.bodyLarge, color = p.textSecondary)
                        inner()
                    }
                })
            if (query.isNotEmpty()) IconAction(stringResource(R.string.component_clear_search), Icons.Default.Close,
                { onQuery("") }, module = ModuleAccent.Vault)
        }
    }
}

@Composable
private fun DashboardRecord(record: VaultRecord, doc: VaultDocument, open: (String) -> Unit, favorite: (String) -> Unit, busy: Boolean) {
    VaultItemCard(record.title, categoryPath(record.categoryId, doc.categories).ifBlank { stringResource(R.string.vault_uncategorized) },
        record.favorite, true, { open(record.id) }, { favorite(record.id) }, enabled = !busy, compact = true)
}

@Composable
private fun queryScopeLabel(tag: String, favorites: Boolean, sort: Int): String = listOfNotNull(
    stringResource(when (sort) { 1 -> R.string.vault_sort_modified; 2 -> R.string.vault_sort_created; 3 -> R.string.vault_sort_category; else -> R.string.vault_sort_name }),
    if (favorites) stringResource(R.string.vault_favorites) else null, tag.takeIf { it.isNotBlank() }).joinToString(" · ")

