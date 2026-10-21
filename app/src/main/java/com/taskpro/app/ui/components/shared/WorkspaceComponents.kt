package com.taskpro.app.ui.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.taskpro.app.R
import com.taskpro.app.ui.theme.*

@Composable
fun WorkspaceCard(title: String, subtitle: String, icon: ImageVector, module: ModuleAccent,
                  onClick: () -> Unit, modifier: Modifier = Modifier, selected: Boolean = false,
                  locked: Boolean = false, enabled: Boolean = true) {
    val lockedLabel = stringResource(R.string.component_locked)
    ComponentCard(onClick, modifier.heightIn(min = 128.dp).semantics { if (locked) stateDescription = lockedLabel },
        module, selected, enabled) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ModuleAccentTile(icon, module)
            Spacer(Modifier.weight(1f))
            Icon(if (locked) Icons.Default.Lock else Icons.AutoMirrored.Filled.ArrowForward, null, tint = module.color())
        }
        Text(title, Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
        Text(subtitle, Modifier.fillMaxWidth(), color = LocalVaurivoColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

// No routes or NavController: the owner maps these callbacks to existing destinations.
enum class WorkspaceTab { Home, Tasks, Vault }

@Composable
fun BottomNavigationBar(selected: WorkspaceTab, onSelect: (WorkspaceTab) -> Unit,
                        modifier: Modifier = Modifier, vaultLocked: Boolean = true,
                        enabled: Boolean = true) {
    val p = LocalVaurivoColors.current
    val locked = stringResource(R.string.component_locked)
    Row(modifier.fillMaxWidth().background(p.surface).navigationBarsPadding().selectableGroup()) {
        WorkspaceTab.entries.forEach { tab ->
            val label = stringResource(when (tab) {
                WorkspaceTab.Home -> R.string.component_home
                WorkspaceTab.Tasks -> R.string.workspace_tasks
                WorkspaceTab.Vault -> R.string.vault_title
            })
            val module = if (tab == WorkspaceTab.Vault) ModuleAccent.Vault else ModuleAccent.Tasks
            val source = remember(tab) { MutableInteractionSource() }
            Column(Modifier.weight(1f).heightIn(min = VaurivoLayout.navigationHeight)
                .componentFocus(source, module, VaurivoShapes.utility)
                .selectable(tab == selected, enabled = enabled, role = Role.Tab,
                    interactionSource = source, indication = ripple(), onClick = { onSelect(tab) })
                .semantics { if (tab == WorkspaceTab.Vault && vaultLocked) stateDescription = locked }
                .padding(horizontal = VaurivoSpacing.extraSmall, vertical = VaurivoSpacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.extraSmall)) {
                val icon = when (tab) {
                    WorkspaceTab.Home -> Icons.Default.GridView
                    WorkspaceTab.Tasks -> Icons.Default.Checklist
                    WorkspaceTab.Vault -> if (vaultLocked) Icons.Default.Lock else Icons.Default.LockOpen
                }
                Icon(icon, null, tint = if (!enabled) p.disabledContent else if (tab == selected) module.color() else p.textSecondary)
                Text(label, Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (!enabled) p.disabledContent else if (tab == selected) p.textPrimary else p.textSecondary)
            }
        }
    }
}
