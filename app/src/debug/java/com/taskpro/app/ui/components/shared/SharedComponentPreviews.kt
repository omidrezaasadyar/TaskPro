package com.taskpro.app.ui.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.taskpro.app.R
import com.taskpro.app.data.Task
import com.taskpro.app.data.TaskStatus
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.theme.*
import com.taskpro.app.vault.*

/** Debug-only component specimens, not application screens. All content is synthetic. */
@Composable
fun SharedComponentSpecimens(page: Int, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var favorite by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(WorkspaceTab.Vault) }
    var checked by remember { mutableStateOf(false) }
    var taskStatus by remember { mutableStateOf(TaskStatus.PENDING) }
    var notice by remember { mutableStateOf("") }
    var dialog by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf(GeneratorOptions()) }
    var candidate by remember { mutableStateOf<GeneratedPasswordCandidate?>(null) }
    var password by remember { mutableStateOf("synthetic-only") }
    Column(modifier.background(LocalVaurivoColors.current.canvas).verticalScroll(rememberScrollState())
        .padding(VaurivoLayout.compactScreenMargin), verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.medium)) {
        when (page) {
            0 -> {
                SectionHeader(stringResource(R.string.workspace_title))
                SearchField(query, { query = it }, stringResource(R.string.search))
                WorkspaceCard(stringResource(R.string.workspace_tasks), stringResource(R.string.workspace_tasks_subtitle), Icons.Default.Checklist,
                    ModuleAccent.Tasks, { notice = "Tasks selected" }, Modifier.fillMaxWidth())
                WorkspaceCard(stringResource(R.string.vault_title), stringResource(R.string.workspace_vault_subtitle), Icons.Default.Lock,
                    ModuleAccent.Vault, { notice = "Vault selected" }, Modifier.fillMaxWidth(), locked = true)
                FilterChip(stringResource(R.string.status_pending), checked, { checked = !checked })
                TaskRow(Task(id = 77, itemId = 42, title = "Synthetic review / بررسی نمونه", notes = "Project notes", dueAt = null, status = taskStatus),
                    { notice = "Edit selected" }, { taskStatus = if (it) TaskStatus.COMPLETED else TaskStatus.PENDING },
                    metadata = "17 Oct 2026", onSnooze = { taskStatus = TaskStatus.SNOOZED })
                BottomNavigationBar(selectedTab, { selectedTab = it })
            }
            1 -> {
                SectionHeader(stringResource(R.string.vault_categories), actionLabel = stringResource(R.string.vault_new_category), onAction = { notice = "Create group selected" }, module = ModuleAccent.Vault)
                VaultGroupCard(VaultCategory(id = "synthetic-group", name = stringResource(R.string.vault_bank), color = 0xff4188ba.toInt(), icon = CategoryIcon.BANK),
                    true, { notice = "Folder selected" }, { notice = "Group menu selected" }, directItems = 12, subgroups = 1)
                VaultItemCard("Synthetic account", stringResource(R.string.vault_bank), favorite, true, { notice = "Item selected" }, { favorite = !favorite }, modifiedLabel = "17 Oct 2026")
                SecureFieldCard(stringResource(R.string.vault_username), "sample@example.test", false, FieldType.EMAIL, 1, true, onCopy = { notice = "Copy requested" }, technical = true)
                SecureFieldCard(stringResource(R.string.vault_password), "synthetic-password-only", true, FieldType.SECRET, 1, true, onCopy = { notice = "Copy requested" })
                PrimaryActionButton(stringResource(R.string.vault_add_item), { notice = "Add selected" }, module = ModuleAccent.Vault, icon = Icons.Default.Add)
            }
            2 -> {
                SecureInputField(stringResource(R.string.vault_password), password, { password = it }, 1, true, type = FieldType.SECRET)
                PasswordGeneratorPanel(options, candidate, { options = it }, {
                    candidate = GeneratedPasswordCandidate(VaultCrypto.generate(options.length, options.lower, options.upper, options.digits, options.symbols), options, 1)
                }, { password = it; candidate = null }, { candidate = null }, 1, true)
                SecurityIndicator(stringResource(R.string.vault_security), score = 2)
            }
            4 -> {
                SectionHeader(stringResource(R.string.vault_categories))
                Row(horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.medium)) {
                    VaultGroupCard(VaultCategory(id = "group-a", name = stringResource(R.string.vault_bank), color = 0xffb95045.toInt(), icon = CategoryIcon.BANK),
                        true, { notice = "Folder selected" }, { notice = "Menu selected" }, Modifier.weight(1f), directItems = 12, subgroups = 1)
                    VaultGroupCard(VaultCategory(id = "group-b", name = stringResource(R.string.vault_email), color = 0xff4188ba.toInt(), icon = CategoryIcon.EMAIL),
                        true, { notice = "Folder selected" }, { notice = "Menu selected" }, Modifier.weight(1f), directItems = 9, subgroups = 0)
                }
                VaultItemCard("Synthetic account", stringResource(R.string.vault_email), favorite, true, { notice = "Item selected" }, { favorite = !favorite })
                SecureFieldCard(stringResource(R.string.vault_url), "https://example.test", false, FieldType.URL, 1, true,
                    onCopy = { notice = "Copy requested" }, onOpen = { notice = "Open requested" })
                SecureFieldCard(stringResource(R.string.vault_notes), "Synthetic notes only", false, FieldType.NOTES, 1, true)
            }
            else -> {
                EmptyState(stringResource(R.string.vault_empty), stringResource(R.string.vault_uncategorized), Icons.Default.FolderOpen,
                    actionLabel = stringResource(R.string.vault_add_item), onAction = { notice = "Add selected" }, module = ModuleAccent.Vault)
                ErrorState(stringResource(R.string.vault_error), retryLabel = stringResource(R.string.save), onRetry = { notice = "Retry selected" })
                LoadingState(stringResource(R.string.component_busy))
                PrimaryActionButton(stringResource(R.string.save), {}, busy = true)
                PrimaryActionButton(stringResource(R.string.save), {}, enabled = false, unavailableReason = stringResource(R.string.vault_validation))
                PrimaryActionButton(stringResource(R.string.delete), { dialog = true })
            }
        }
        if (notice.isNotEmpty()) Text(notice, color = LocalVaurivoColors.current.textSecondary)
    }
    if (dialog) ConfirmationDialog(stringResource(R.string.delete), stringResource(R.string.vault_delete_record), stringResource(R.string.delete),
        { dialog = false; notice = "Confirmed" }, { dialog = false }, destructive = true)
}

@Preview(name = "Dark workspace", widthDp = 360, heightDp = 1000)
@Composable private fun WorkspacePreview() { TaskProTheme(ThemeMode.DARK) { SharedComponentSpecimens(0) } }
@Preview(name = "Dark vault", widthDp = 360, heightDp = 1000)
@Composable private fun VaultPreview() { TaskProTheme(ThemeMode.DARK) { SharedComponentSpecimens(1) } }
@Preview(name = "Light vault", widthDp = 360, heightDp = 1000)
@Composable private fun LightPreview() { TaskProTheme(ThemeMode.LIGHT) { SharedComponentSpecimens(1) } }
@Preview(name = "Persian large", locale = "fa", fontScale = 2f, widthDp = 320, heightDp = 1100)
@Composable private fun PersianPreview() { TaskProTheme(ThemeMode.DARK) { SharedComponentSpecimens(1) } }
@Preview(name = "Generator", widthDp = 360, heightDp = 1100)
@Composable private fun GeneratorPreview() { TaskProTheme(ThemeMode.DARK) { SharedComponentSpecimens(2) } }
@Preview(name = "States", widthDp = 360, heightDp = 1100)
@Composable private fun StatePreview() { TaskProTheme(ThemeMode.DARK) { SharedComponentSpecimens(3) } }
@Preview(name = "Dark folder pair", widthDp = 400, heightDp = 1000)
@Composable private fun FolderPreview() { TaskProTheme(ThemeMode.DARK) { SharedComponentSpecimens(4) } }
