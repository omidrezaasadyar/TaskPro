package com.taskpro.app.ui.screens.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.nulabinc.zxcvbn.Zxcvbn
import com.taskpro.app.R
import com.taskpro.app.ui.components.shared.GeneratedPasswordCandidate
import com.taskpro.app.ui.components.shared.GeneratorOptions
import com.taskpro.app.ui.components.shared.ModuleAccent
import com.taskpro.app.ui.components.shared.PasswordGeneratorPanel
import com.taskpro.app.ui.components.shared.PrimaryActionButton
import com.taskpro.app.ui.components.shared.SecureInputField
import com.taskpro.app.ui.theme.LocalVaurivoColors
import com.taskpro.app.ui.theme.VaurivoLayout
import com.taskpro.app.ui.theme.VaurivoSpacing
import com.taskpro.app.vault.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditor(initial: VaultRecord, document: VaultDocument, busy: Boolean, onDismiss: () -> Unit,
                 sessionKey: Any = initial.id, onSave: (VaultRecord) -> Unit) {
    var record by remember { mutableStateOf(initial) }
    var tags by remember { mutableStateOf(initial.tags.joinToString(", ")) }
    var validation by remember { mutableStateOf(false) }
    var generator by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf(GeneratorOptions()) }
    var candidate by remember { mutableStateOf<GeneratedPasswordCandidate?>(null) }
    var submitted by remember { mutableStateOf(false) }
    var strength by remember { mutableIntStateOf(0) }
    val isEdit = document.records.any { it.id == initial.id }
    val palette = LocalVaurivoColors.current
    val password = record.values["password"].orEmpty()
    LaunchedEffect(password) {
        delay(300)
        strength = withContext(Dispatchers.Default) { Zxcvbn().measure(password.take(128)).score }
    }
    val save: () -> Unit = {
        if (!busy && !submitted) {
            val updated = record.copy(tags = tags.split(',', '،').map(String::trim).filter(String::isNotEmpty).distinct(),
                modifiedAt = maxOf(System.currentTimeMillis(), record.createdAt))
            validation = runCatching { document.copy(records = document.records.filterNot { it.id == updated.id } + updated).validate() }.isFailure
            if (!validation) { submitted = true; onSave(updated) }
        }
    }
    Dialog(onDismissRequest = { if (!busy && !submitted) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)) {
        Surface(Modifier.fillMaxWidth().widthIn(max = 640.dp).fillMaxHeight().imePadding(), color = palette.canvas) {
        Scaffold(modifier = Modifier.vaultActivity(), containerColor = palette.canvas,
            topBar = { TopAppBar(title = { Text(stringResource(if (isEdit) R.string.vault_edit_item else R.string.vault_add_item)) },
                navigationIcon = { IconButton(onClick = onDismiss, enabled = !busy && !submitted) {
                    Icon(Icons.Default.Close, stringResource(R.string.cancel))
                } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.canvas)) },
            bottomBar = {
                Surface(color = palette.surface) {
                    PrimaryActionButton(stringResource(R.string.vault_save_item), save,
                        Modifier.fillMaxWidth().padding(horizontal = VaurivoLayout.compactScreenMargin, vertical = VaurivoSpacing.small),
                        module = ModuleAccent.Vault, icon = Icons.Default.Check, enabled = !submitted, busy = busy)
                }
            }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(
                start = VaurivoLayout.compactScreenMargin, end = VaurivoLayout.compactScreenMargin,
                top = VaurivoSpacing.medium, bottom = VaurivoSpacing.section),
                verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.medium)) {
                item {
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (validation) Text(stringResource(R.string.vault_validation), color = MaterialTheme.colorScheme.error)
                    EditorHeading(stringResource(R.string.vault_item_details))
                }
                item {
                    VaultInput(stringResource(R.string.vault_record_title), record.title, { record = record.copy(title = it) })
                }
                item { VaultChoice(stringResource(R.string.vault_category), record.categoryId,
                    listOf(null to stringResource(R.string.vault_uncategorized)) + document.categories.map { it.id to categoryPath(it.id, document.categories) }) {
                    record = record.copy(categoryId = it)
                } }
                item { EditorHeading(stringResource(R.string.vault_sign_in_details)) }
                itemsIndexed(listOf("username", "email", "password", "url")) { _, field ->
                    if (field == "password") {
                        SecureInputField(stringResource(R.string.vault_password), password,
                            { record = record.copy(values = record.values + (field to it)) }, sessionKey, true,
                            type = FieldType.SECRET, busy = busy)
                        TextButton(onClick = { generator = !generator; candidate = null }, enabled = !busy,
                            modifier = Modifier.heightIn(min = VaurivoLayout.minimumTouchTarget)) {
                            Icon(Icons.Default.AutoFixHigh, null); Spacer(Modifier.width(VaurivoSpacing.small))
                            Text(stringResource(R.string.vault_generate))
                        }
                        if (password.isNotBlank()) Text(stringResource(R.string.vault_strength, strength), style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary)
                        if (generator) PasswordGeneratorPanel(options, candidate,
                            { options = it; candidate = null },
                            { candidate = GeneratedPasswordCandidate(VaultCrypto.generate(options.length, options.lower, options.upper, options.digits, options.symbols), options, sessionKey) },
                            { record = record.copy(values = record.values + ("password" to it)); candidate = null; generator = false },
                            { candidate = null; generator = false }, sessionKey, true, busy = busy)
                    } else VaultInput(stringResource(fieldLabel(field)), record.values[field].orEmpty(),
                        { record = record.copy(values = record.values + (field to it)) },
                        type = when (field) { "email" -> KeyboardType.Email; "url" -> KeyboardType.Uri; else -> KeyboardType.Text })
                }
                item { EditorHeading(stringResource(R.string.vault_account_details)) }
                itemsIndexed(listOf("phone", "reference", "notes")) { _, field ->
                    VaultInput(stringResource(fieldLabel(field)), record.values[field].orEmpty(),
                        { record = record.copy(values = record.values + (field to it)) }, multiline = field == "notes",
                        type = if (field == "phone") KeyboardType.Phone else KeyboardType.Text)
                }
                item { EditorHeading(stringResource(R.string.vault_organization)) }
                item {
                    VaultInput(stringResource(R.string.vault_tags), tags, { tags = it })
                    VaultToggle(stringResource(R.string.vault_favorite), record.favorite) { record = record.copy(favorite = it) }
                }
                item { EditorHeading(stringResource(R.string.vault_custom)) }
                itemsIndexed(record.custom, key = { _, f -> f.id }) { index, field ->
                    fun update(value: CustomField) { record = record.copy(custom = record.custom.mapIndexed { i, f -> if (i == index) value else f }) }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            VaultInput(stringResource(R.string.vault_field_label), field.label, { update(field.copy(label = it)) }, modifier = Modifier.weight(1f))
                            IconButton(onClick = { record = record.copy(custom = record.custom.filterNot { it.id == field.id }) },
                                modifier = Modifier.size(VaurivoLayout.minimumTouchTarget)) { Icon(Icons.Default.DeleteOutline, stringResource(R.string.delete)) }
                        }
                        VaultChoice(stringResource(R.string.vault_field_type), field.type, FieldType.entries.map { it to stringResource(typeLabel(it)) }) { update(field.copy(type = it)) }
                        if (field.secret) SecureInputField(stringResource(typeLabel(field.type)), field.value,
                            { update(field.copy(value = it)) }, sessionKey, true, type = field.type, sensitive = true, busy = busy)
                        else VaultInput(stringResource(typeLabel(field.type)), field.value, { update(field.copy(value = it)) },
                            multiline = field.type == FieldType.NOTES,
                            type = when (field.type) { FieldType.NUMBER -> KeyboardType.Decimal; FieldType.URL -> KeyboardType.Uri; FieldType.EMAIL -> KeyboardType.Email; else -> KeyboardType.Text })
                        VaultToggle(stringResource(R.string.vault_sensitive), field.sensitive) { update(field.copy(sensitive = it)) }
                        HorizontalDivider()
                    }
                }
                item { TextButton(onClick = { record = record.copy(custom = record.custom + CustomField()) },
                    modifier = Modifier.heightIn(min = VaurivoLayout.minimumTouchTarget)) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(VaurivoSpacing.small)); Text(stringResource(R.string.vault_add_field))
                } }
            }
        }
        }
    }
}

@Composable
private fun EditorHeading(label: String) {
    Text(label, Modifier.fillMaxWidth().padding(top = VaurivoSpacing.small).semantics { heading() },
        style = MaterialTheme.typography.titleSmall, color = LocalVaurivoColors.current.textSecondary)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditor(initial: VaultCategory, categories: List<VaultCategory>, onDismiss: () -> Unit, onSave: (VaultCategory) -> Unit) {
    var category by remember { mutableStateOf(initial) }
    var hex by remember { mutableStateOf("%06X".format(initial.color and 0xffffff)) }
    val colors = listOf(0xff2563eb, 0xff0d9488, 0xff9333ea, 0xffbe123c, 0xff15803d, 0xffb45309, 0xff475569, 0xff0891b2)
    val disallowed = descendants(initial.id, categories)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)) {
        Scaffold(modifier = Modifier.vaultActivity(), topBar = {
            TopAppBar(title = { Text(stringResource(R.string.vault_category)) }, navigationIcon = {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, stringResource(R.string.cancel)) }
            }, actions = {
                TextButton(enabled = category.name.isNotBlank() && hex.matches(Regex("[0-9a-fA-F]{6}")), onClick = {
                    onSave(category.copy(color = (0xff000000L or hex.toLong(16)).toInt()))
                }) { Text(stringResource(R.string.save)) }
            })
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VaultInput(stringResource(R.string.vault_category_name), category.name, { category = category.copy(name = it) })
                VaultChoice(stringResource(R.string.vault_parent), category.parentId,
                    listOf(null to stringResource(R.string.vault_all)) + categories.filterNot { it.id in disallowed }.map { it.id to categoryPath(it.id, categories) }) {
                    category = category.copy(parentId = it)
                }
                VaultChoice(stringResource(R.string.vault_icon), category.icon, CategoryIcon.entries.map { it to stringResource(categoryLabel(it)) }) { category = category.copy(icon = it) }
                FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = 4) {
                    colors.forEach { color ->
                        val label = "${stringResource(R.string.vault_color)} #%06X".format(color and 0xffffff)
                        Box(Modifier.size(48.dp).semantics { contentDescription = label }
                            .clickable { category = category.copy(color = color.toInt()); hex = "%06X".format(color and 0xffffff) }, contentAlignment = Alignment.Center) {
                            Box(Modifier.size(30.dp).background(Color(color), CircleShape)
                                .border(if (category.color == color.toInt()) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape))
                        }
                    }
                }
                VaultInput("${stringResource(R.string.vault_color)} (#RRGGBB)", hex, { hex = it.removePrefix("#").take(6) })
            }
        }
    }
}
