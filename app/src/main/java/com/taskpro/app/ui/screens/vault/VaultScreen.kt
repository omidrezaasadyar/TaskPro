package com.taskpro.app.ui.screens.vault

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskpro.app.R
import com.taskpro.app.ui.screens.WorkspaceBackdrop
import com.taskpro.app.vault.*
import com.taskpro.app.ui.components.shared.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(repository: VaultRepository, clipboard: VaultClipboard, onWorkspace: () -> Unit, onSettings: () -> Unit,
                deviceUnlock: VaultDeviceUnlock? = null) {
    val state by repository.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var loading by remember { mutableStateOf(!state.exists) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    var notice by remember { mutableStateOf<Int?>(null) }
    var creatingMaster by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun run(block: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null; notice = null
        scope.launch {
            try { block() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = if (repository.state.value.document == null) R.string.vault_unlock_error else R.string.vault_error }
            finally { busy = false }
        }
    }
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        repository.setVisible(true)
        onDispose { repository.setVisible(false); clipboard.clearOwned(); window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    LaunchedEffect(Unit) {
        try { if (!repository.state.value.exists) repository.refresh() }
        catch (e: Exception) { error = R.string.vault_error }
        finally { loading = false }
    }
    LaunchedEffect(state.generation, state.document == null) {
        creatingMaster = false
        if (state.document == null) clipboard.clearOwned()
    }
    Scaffold(containerColor = if (state.exists && state.document == null) com.taskpro.app.ui.theme.LocalVaurivoColors.current.canvas else MaterialTheme.colorScheme.background, topBar = {
        if (!state.exists || state.pendingRecovery != null) {
        TopAppBar(title = { Text(stringResource(R.string.vault_title)) },
            navigationIcon = { IconButton(onClick = onWorkspace) { Icon(Icons.Default.Apps, stringResource(R.string.workspace_switch)) } },
            actions = {
                IconButton(onClick = { repository.lock(); onSettings() }) { Icon(Icons.Default.Settings, stringResource(R.string.settings)) }
                if (state.document != null) IconButton(onClick = repository::lock) { Icon(Icons.Default.Lock, stringResource(R.string.vault_lock)) }
            })
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
            else key(state.generation) {
                val doc = state.document
                if (doc == null) {
                    if (!state.exists || creatingMaster) VaultMasterSetup(repository, state.exists, { creatingMaster = true }, { creatingMaster = false; onWorkspace() })
                    else VaultUnlockScreen(repository, deviceUnlock, busy, error, notice,
                        unlock = { password -> run { repository.unlock(password) } },
                        recover = { recovery, password -> run {
                            repository.recoverPassword(recovery, password); notice = R.string.vault_recovery_success
                        } }, createMaster = { creatingMaster = true })
                } else if (state.pendingRecovery != null) {
                    RecoverySetup(busy, { run { repository.confirmRecoverySaved() } }, { value ->
                        val message = if (runCatching { clipboard.copy(value) }.isSuccess) R.string.vault_copied else R.string.vault_error
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                    },
                        initialKey = state.pendingRecovery, generate = { _, _ -> })
                } else VaultWorkspace(doc, state.generation, busy, repository, clipboard, onWorkspace, onSettings, ::run)
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
    error?.takeIf { state.document != null }?.let { message -> AlertDialog(onDismissRequest = { error = null },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
        text = { Text(stringResource(message)) }, confirmButton = { TextButton(onClick = { error = null }) { Text(stringResource(R.string.ok)) } }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultWorkspace(doc: VaultDocument, generation: Long, busy: Boolean, repository: VaultRepository, clipboard: VaultClipboard,
                           onWorkspace: () -> Unit, onSettings: () -> Unit, launchAction: (suspend () -> Unit) -> Unit) {
    val binding = rememberDisplaySession(generation, true)
    fun current(): Boolean {
        repository.checkInactivity()
        val live = repository.state.value
        return binding.matches(generation) && live.generation == generation && live.document != null && live.pendingRecovery == null
    }
    fun run(block: suspend () -> Unit) { if (current()) launchAction { if (current()) block() } }
    suspend fun edit(transform: (VaultDocument) -> VaultDocument) {
        repository.edit { value -> check(current()) { "Session ended" }; transform(value) }
    }
    var query by remember { mutableStateOf("") }; var tag by remember { mutableStateOf("") }
    var favorites by remember { mutableStateOf(false) }; var sort by remember { mutableIntStateOf(0) }
    var category by remember { mutableStateOf<String?>(null) }
    var allRecords by remember { mutableStateOf(false) }
    var drawer by remember { mutableStateOf(false) }
    var collapsed by remember { mutableStateOf(emptySet<String>()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<VaultRecord?>(null) }
    var categoryEdit by remember { mutableStateOf<VaultCategory?>(null) }
    var deleteRecord by remember { mutableStateOf<String?>(null) }
    var deleteCategory by remember { mutableStateOf<String?>(null) }
    var security by remember { mutableStateOf(false) }
    var recoverySetup by remember { mutableStateOf(false) }
    val folderAlpha = remember { Animatable(1f) }
    LaunchedEffect(category) { folderAlpha.snapTo(0f); folderAlpha.animateTo(1f, tween(180)) }
    val searchFocus = remember { FocusRequester() }
    var searchRequest by remember { mutableIntStateOf(0) }
    val workspaceFocus = remember { FocusRequester() }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun copy(value: String) {
        if (!current()) return
        runCatching { clipboard.copy(value) }.onSuccess {
            android.widget.Toast.makeText(context, R.string.vault_copied, android.widget.Toast.LENGTH_LONG).show()
        }.onFailure {
            android.widget.Toast.makeText(context, R.string.vault_error, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    fun newRecord() { if (current()) editing = VaultRecord(categoryId = category?.takeUnless { it == "" }) }
    fun newCategory() {
        if (!current()) return
        val palette = listOf(0xff2563eb, 0xff0d9488, 0xffa855a0, 0xffb45309, 0xff15803d, 0xff64748b)
        categoryEdit = VaultCategory(name = "", parentId = category?.takeUnless { it == "" },
            color = palette.minBy { color -> doc.categories.count { it.color == color.toInt() } }.toInt(),
            position = (doc.categories.maxOfOrNull { it.position } ?: -1) + 1)
    }
    val folders = doc.categories.filter {
        if (query.isNotBlank()) categoryPath(it.id, doc.categories).contains(query, true)
        else category != "" && it.parentId == category
    }.sortedBy { it.position }
    val filtered = doc.records.filter { record ->
        (query.isNotBlank() || category == null || (category == "" && record.categoryId == null) || record.categoryId == category) &&
            (!favorites || record.favorite) && (tag.isBlank() || record.tags.any { it.contains(tag, true) }) &&
            record.matches(query, categoryPath(record.categoryId, doc.categories))
    }.let { list -> when (sort) {
        1 -> list.sortedByDescending { it.modifiedAt }; 2 -> list.sortedByDescending { it.createdAt }
        3 -> list.sortedBy { categoryPath(it.categoryId, doc.categories).lowercase() }
        else -> list.sortedBy { it.title.lowercase() }
    } }
    fun reorder(c: VaultCategory, delta: Int) = run {
        edit { current ->
            val siblings = current.categories.filter { it.parentId == c.parentId }.sortedBy { it.position }.toMutableList()
            val index = siblings.indexOfFirst { it.id == c.id }
            val other = index + delta
            if (index >= 0 && other in siblings.indices) java.util.Collections.swap(siblings, index, other)
            val positions = siblings.mapIndexed { i, item -> item.id to i }.toMap()
            current.copy(categories = current.categories.map { it.copy(position = positions[it.id] ?: it.position) })
        }
    }
    fun toggleFavorite(id: String) = run {
        edit { current -> current.copy(records = current.records.map {
            if (it.id == id) it.copy(favorite = !it.favorite, modifiedAt = maxOf(System.currentTimeMillis(), it.createdAt)) else it
        }) }
    }
    val tree: @Composable () -> Unit = {
        CategoryTree(doc, category, collapsed, onSelect = { if (current()) { category = it; allRecords = it == null; drawer = false; selected = null } },
            onCollapse = { if (current()) collapsed = if (it in collapsed) collapsed - it else collapsed + it },
            onEdit = { if (current()) { drawer = false; categoryEdit = it } }, onDelete = { if (current()) { drawer = false; deleteCategory = it.id } }, onReorder = ::reorder,
            onStarter = { run { edit { current -> current.copy(categories = current.categories +
                listOf(CategoryIcon.BANK, CategoryIcon.EMAIL, CategoryIcon.WEB, CategoryIcon.BUSINESS, CategoryIcon.SERVER).mapIndexed { index, icon ->
                    VaultCategory(name = context.getString(categoryLabel(icon)), icon = icon, position = current.categories.size + index,
                        color = listOf(0xff2563eb, 0xff9333ea, 0xff0d9488, 0xff15803d, 0xffb45309)[index].toInt())
                }) } } },
            onNew = { drawer = false; newCategory() })
    }
    Box(Modifier.fillMaxSize().background(com.taskpro.app.ui.theme.LocalVaurivoColors.current.canvas)) {
    Scaffold(containerColor = Color.Transparent, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).onPreviewKeyEvent {
            if (it.type == KeyEventType.KeyDown && it.isCtrlPressed) when {
                it.key == Key.F -> { if (current()) searchRequest++; true }
                it.key == Key.N -> { newRecord(); true }
                it.key == Key.L -> { if (current()) repository.lock(); true }
                it.key == Key.H && it.isShiftPressed -> { if (current()) onWorkspace(); true }
                else -> false
            } else false
        }.focusRequester(workspaceFocus).focusable()) {
            LaunchedEffect(Unit) { workspaceFocus.requestFocus() }
            val wide = maxWidth >= 840.dp
            Row(Modifier.fillMaxSize()) {
                if (wide) Box(Modifier.width(260.dp).fillMaxHeight()) { tree() }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    VaultDashboard(doc, folders, filtered, query, { if (current()) query = it }, tag, { if (current()) tag = it },
                        favorites, { if (current()) favorites = it }, sort, { if (current()) sort = it }, category, allRecords,
                        onAll = { if (current()) { category = null; allRecords = true; selected = null } },
                        onParent = { if (current()) { if (query.isNotBlank()) query = "" else { category = doc.categories.find { it.id == category }?.parentId; allRecords = false }; selected = null } },
                        onFolder = { if (current()) { category = it; query = ""; selected = null } },
                        onCategories = { if (current()) drawer = true }, onNewGroup = ::newCategory,
                        onEditGroup = { if (current()) categoryEdit = it }, onDeleteGroup = { if (current()) deleteCategory = it.id },
                        onReorder = ::reorder, onRecord = { if (current()) selected = it },
                        onFavorite = ::toggleFavorite,
                        onNewRecord = ::newRecord, onLock = { if (current()) repository.lock() },
                        onSecurity = { if (current()) security = true }, onSettings = { if (current()) { repository.lock(); onSettings() } },
                        onWorkspace = { if (current()) onWorkspace() }, searchModifier = Modifier.focusRequester(searchFocus),
                        searchRequest = searchRequest, onSearchReady = { if (current()) searchFocus.requestFocus() },
                        modifier = Modifier.graphicsLayer { alpha = folderAlpha.value }, busy = busy)
                }
                if (wide && selected != null) {
                    doc.records.find { it.id == selected }?.let { record ->
                        Box(Modifier.width(320.dp).fillMaxHeight()) { RecordDetails(record, doc, ::current, ::copy,
                            { selected = null }, ::toggleFavorite,
                            { if (current()) editing = record }, { if (current()) editing = record.copy(id = java.util.UUID.randomUUID().toString(), createdAt = System.currentTimeMillis(), modifiedAt = System.currentTimeMillis()) },
                            { if (current()) deleteRecord = record.id }) }
                    }
                }
            }
            if (!wide && selected != null) doc.records.find { it.id == selected }?.let { record ->
                Dialog(onDismissRequest = { selected = null }, properties = DialogProperties(usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)) {
                    Surface(color = com.taskpro.app.ui.theme.LocalVaurivoColors.current.canvas) { RecordDetails(record, doc, ::current, ::copy, { selected = null },
                        ::toggleFavorite,
                        { if (current()) editing = record },
                        { if (current()) editing = record.copy(id = java.util.UUID.randomUUID().toString(), createdAt = System.currentTimeMillis(), modifiedAt = System.currentTimeMillis()) },
                        { if (current()) deleteRecord = record.id }) }
                }
            }
        }
    }
    }
    BackHandler((category != null || allRecords) && selected == null && editing == null && categoryEdit == null && !security && !recoverySetup && !drawer) {
        category = doc.categories.find { it.id == category }?.parentId; allRecords = false
    }
    if (drawer) {
        val configuration = LocalConfiguration.current
        val panelHeight = minOf(620, (configuration.screenHeightDp * 0.8f).toInt()).dp
        val panelWidth = minOf(420, configuration.screenWidthDp - 24).dp
        Dialog(onDismissRequest = { drawer = false }, properties = DialogProperties(usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)) {
            Surface(Modifier.vaultActivity().size(panelWidth, panelHeight), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) { tree() }
        }
    }
    editing?.let { record -> RecordEditor(record, doc, busy, { editing = null }, generation) { updated -> run {
        edit { it.copy(records = it.records.filterNot { r -> r.id == updated.id } + updated) }; editing = null; selected = updated.id
    } } }
    categoryEdit?.let { initial -> CategoryEditor(initial, doc.categories, { categoryEdit = null }) { updated -> run {
        edit { it.copy(categories = it.categories.filterNot { c -> c.id == updated.id } + updated) }; categoryEdit = null
    } } }
    if (deleteRecord != null || deleteCategory != null) AlertDialog(onDismissRequest = { deleteRecord = null; deleteCategory = null },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
        text = { Text(stringResource(if (deleteRecord != null) R.string.vault_delete_record else R.string.vault_delete_category)) },
        confirmButton = { TextButton(enabled = !busy, onClick = { run {
            edit { current ->
                if (deleteRecord != null) current.copy(records = current.records.filterNot { it.id == deleteRecord })
                else {
                    val ids = descendants(checkNotNull(deleteCategory), current.categories)
                    current.copy(categories = current.categories.filterNot { it.id in ids },
                        records = current.records.map { if (it.categoryId in ids) it.copy(categoryId = null, modifiedAt = maxOf(System.currentTimeMillis(), it.createdAt)) else it })
                }
            }
            deleteRecord = null; deleteCategory = null; selected = null; category = null
        } }) { Text(stringResource(R.string.delete)) } }, dismissButton = { TextButton(onClick = { deleteRecord = null; deleteCategory = null }) { Text(stringResource(R.string.cancel)) } })
    if (security) VaultSecurity(doc, busy, { security = false }, { security = false; recoverySetup = true }, { seconds -> run { edit { it.copy(autoLockSeconds = seconds) } } },
        { old, new -> run { repository.changePassword(old, new); security = false } })
    if (recoverySetup) RecoverySetup(busy, { recoverySetup = false; security = true }, ::copy,
        view = { password, show -> run { show(repository.setupRecovery(password, viewOnly = true)) } }) { password, show ->
        run { show(repository.setupRecovery(password)) }
    }
    BackHandler(selected != null && editing == null) { selected = null }
}

@Composable
private fun CategoryTree(doc: VaultDocument, selected: String?, collapsed: Set<String>, onSelect: (String?) -> Unit,
                         onCollapse: (String) -> Unit, onEdit: (VaultCategory) -> Unit, onDelete: (VaultCategory) -> Unit,
                         onReorder: (VaultCategory, Int) -> Unit, onStarter: () -> Unit, onNew: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val rows = mutableListOf<Pair<VaultCategory, Int>>()
    val queue = java.util.ArrayDeque<Pair<VaultCategory, Int>>()
    doc.categories.filter { it.parentId == null }.sortedByDescending { it.position }.forEach { queue.addFirst(it to 0) }
    while (queue.isNotEmpty()) {
        val pair = queue.removeFirst()
        if (query.isBlank() || categoryPath(pair.first.id, doc.categories).contains(query, true)) rows.add(pair)
        if (pair.first.id !in collapsed || query.isNotBlank()) doc.categories.filter { it.parentId == pair.first.id }
            .sortedByDescending { it.position }.forEach { queue.addFirst(it to pair.second + 1) }
    }
    Column(Modifier.fillMaxHeight().padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.vault_categories), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onNew, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.CreateNewFolder, stringResource(R.string.vault_new_category)) }
        }
        OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.search)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { onSelect(null) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.vault_all)) }
        TextButton(onClick = { onSelect("") }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.vault_uncategorized)) }
        if (doc.categories.isEmpty()) TextButton(onClick = onStarter, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.vault_starter)) }
        LazyColumn(Modifier.weight(1f)) {
            items(rows, key = { it.first.id }) { (c, depth) ->
                var menu by remember { mutableStateOf(false) }
                Column(Modifier.fillMaxWidth().background(if (selected == c.id) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                    .padding(start = minOf(depth, 5).times(10).dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onCollapse(c.id) }, modifier = Modifier.size(48.dp)) {
                            Icon(if (c.id in collapsed) Icons.Default.ChevronRight else Icons.Default.ExpandMore, stringResource(R.string.vault_expand))
                        }
                        Row(Modifier.weight(1f).heightIn(min = 48.dp).clickable { onSelect(c.id) }, verticalAlignment = Alignment.CenterVertically) {
                            Icon(categoryIcon(c.icon), null, Modifier.size(20.dp), tint = Color(c.color))
                            Text(c.name, Modifier.weight(1f).padding(horizontal = 6.dp))
                            Text(doc.records.count { it.categoryId in descendants(c.id, doc.categories) }.toString(), style = MaterialTheme.typography.labelSmall)
                        }
                        Box {
                            IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.MoreVert, stringResource(R.string.menu)) }
                            DropdownMenu(menu, { menu = false }) {
                                listOf(R.string.vault_edit to { onEdit(c) }, R.string.vault_move_up to { onReorder(c, -1) },
                                    R.string.vault_move_down to { onReorder(c, 1) }, R.string.delete to { onDelete(c) }).forEach { (label, action) ->
                                    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { action(); menu = false })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordDetails(record: VaultRecord, doc: VaultDocument, authorized: () -> Boolean, copy: (String) -> Unit, close: () -> Unit,
                          favorite: (String) -> Unit,
                          edit: () -> Unit, duplicate: () -> Unit, delete: () -> Unit) {
    val context = LocalContext.current
    val p = com.taskpro.app.ui.theme.LocalVaurivoColors.current
    val group = doc.categories.find { it.id == record.categoryId }
    var menu by remember(record.id) { mutableStateOf(false) }
    val openWebsite: (String) -> Unit = { value ->
        if (authorized() && VaultDocument.validUrl(value)) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value))) }.onFailure {
            android.widget.Toast.makeText(context, R.string.vault_error, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    Column(Modifier.vaultActivity().fillMaxSize().background(p.canvas).verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconAction(stringResource(R.string.back), Icons.AutoMirrored.Filled.ArrowBack, close, module = ModuleAccent.Vault)
            Spacer(Modifier.weight(1f))
            IconAction(stringResource(R.string.vault_favorite), if (record.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                { favorite(record.id) }, Modifier.semantics {
                    stateDescription = context.getString(if (record.favorite) R.string.component_favorite else R.string.component_not_favorite)
                }, module = ModuleAccent.Vault)
            IconAction(stringResource(R.string.vault_edit), Icons.Default.Edit, edit, module = ModuleAccent.Vault)
            Box {
                IconAction(stringResource(R.string.menu), Icons.Default.MoreVert, { menu = true }, module = ModuleAccent.Vault)
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.vault_duplicate)) }, onClick = { menu = false; duplicate() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { menu = false; delete() })
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (group != null) GroupIcon(group, size = 48.dp)
            else Icon(Icons.Default.Key, null, Modifier.size(40.dp), tint = p.vaultAccent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(record.title, Modifier.fillMaxWidth().semantics { heading() }, style = MaterialTheme.typography.titleLarge,
                    color = p.textPrimary)
                Text(categoryPath(record.categoryId, doc.categories).ifBlank { stringResource(R.string.vault_uncategorized) },
                    Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
            }
        }
        key(record.id) {
            val sections = listOf(
                R.string.vault_sign_in_details to listOf("username", "email", "password"),
                R.string.vault_account_details to listOf("url", "phone", "reference"),
                R.string.vault_notes to listOf("notes")
            )
            sections.forEach { (title, keys) ->
                if (keys.any { !record.values[it].isNullOrEmpty() }) {
                    SectionHeader(stringResource(title), module = ModuleAccent.Vault)
                    keys.forEach { field ->
                        val value = record.values[field].orEmpty()
                        if (value.isNotEmpty()) key(field) {
                            val type = when (field) {
                                "password" -> FieldType.SECRET
                                "url" -> FieldType.URL
                                "email" -> FieldType.EMAIL
                                "notes" -> FieldType.NOTES
                                else -> FieldType.TEXT
                            }
                            SecureFieldCard(stringResource(fieldLabel(field)), value, field == "password", type,
                                sessionKey = record.id, active = true, onCopy = copy,
                                onOpen = if (type == FieldType.URL) openWebsite else null,
                                technical = field != "notes")
                        }
                    }
                }
            }
            if (record.custom.any { it.value.isNotEmpty() }) {
                SectionHeader(stringResource(R.string.vault_custom), module = ModuleAccent.Vault)
                record.custom.filter { it.value.isNotEmpty() }.forEach { field -> key(field.id) {
                    SecureFieldCard(field.label, field.value, field.secret, field.type, sessionKey = record.id,
                        active = true, onCopy = copy, onOpen = if (field.type == FieldType.URL && !field.secret) openWebsite else null,
                        technical = field.type in listOf(FieldType.EMAIL, FieldType.NUMBER, FieldType.DATE))
                } }
            }
        }
        if (record.tags.isNotEmpty()) {
            SectionHeader(stringResource(R.string.vault_record_tags), module = ModuleAccent.Vault)
            Text(record.tags.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
        }
        HorizontalDivider(color = p.borderSubtle)
        Text("${stringResource(R.string.vault_created)}: ${DateFormat.getDateTimeInstance().format(Date(record.createdAt))}",
            style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
        Text("${stringResource(R.string.vault_modified)}: ${DateFormat.getDateTimeInstance().format(Date(record.modifiedAt))}",
            style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Shield, null, Modifier.size(16.dp), tint = p.textSecondary)
            Text(stringResource(R.string.vault_detail_privacy), Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultSecurity(doc: VaultDocument, busy: Boolean, close: () -> Unit, recovery: () -> Unit, timeout: (Int) -> Unit, change: (CharArray, CharArray) -> Unit) {
    var old by remember { mutableStateOf("") }; var new by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
    Dialog(onDismissRequest = { if (!busy) close() }, properties = DialogProperties(usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)) {
        WorkspaceBackdrop(Modifier.fillMaxSize()) {
        Scaffold(containerColor = Color.Transparent, modifier = Modifier.vaultActivity(), topBar = { TopAppBar(title = { Text(stringResource(R.string.vault_security)) }, navigationIcon = {
            IconButton(enabled = !busy, onClick = close) { Icon(Icons.Default.Close, stringResource(R.string.cancel)) }
        }) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                VaultChoice(stringResource(R.string.vault_autolock), doc.autoLockSeconds, listOf(30, 60, 120, 300, 600).map { it to it.toString() }, timeout)
                Text(stringResource(R.string.vault_password_notice))
                Text(stringResource(R.string.vault_recovery_key), style = MaterialTheme.typography.titleMedium)
                TextButton(enabled = !busy, onClick = recovery) { Icon(Icons.Default.Key, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.vault_recovery_setup)) }
                HorizontalDivider()
                VaultInput(stringResource(R.string.vault_old_password), old, { old = it.take(1024) }, secret = true)
                VaultInput(stringResource(R.string.vault_new_password), new, { new = it.take(1024) }, secret = true)
                PasswordStrength(new)
                VaultInput(stringResource(R.string.vault_confirm_password), confirm, { confirm = it.take(1024) }, secret = true)
                Button(enabled = !busy && old.isNotBlank() && new.length >= 12 && new == confirm,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), onClick = {
                        val previous = old.toCharArray(); val next = new.toCharArray(); old = ""; new = ""; confirm = ""; change(previous, next)
                    }) { Text(stringResource(R.string.vault_change_password)) }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
        }
    }
}
