package com.taskpro.app.ui.components.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.taskpro.app.R
import com.taskpro.app.ui.screens.vault.categoryIcon
import com.taskpro.app.ui.theme.*
import com.taskpro.app.vault.VaultCategory
import com.taskpro.app.vault.VaultDocument
import com.taskpro.app.vault.FieldType

@Composable
fun GroupIcon(category: VaultCategory, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = VaurivoLayout.minimumTouchTarget) {
    val color = Color(category.color).copy(alpha = 1f)
    // Rendering contrast only: never rewrite the stored arbitrary group color.
    val foreground = if (color.luminance() > .179f) Color.Black else Color.White
    Surface(modifier.size(size), color = color, contentColor = foreground, shape = VaurivoShapes.control) {
        Box(contentAlignment = Alignment.Center) { Icon(categoryIcon(category.icon), null, Modifier.size(VaurivoLayout.iconSize)) }
    }
}

@Composable
fun VaultGroupCard(category: VaultCategory, active: Boolean, onOpen: () -> Unit,
                   onMenu: () -> Unit, modifier: Modifier = Modifier, directItems: Int? = null,
                   subgroups: Int? = null, selected: Boolean = false, enabled: Boolean = true, compact: Boolean = false) {
    if (!active) return
    require(directItems == null || directItems >= 0)
    require(subgroups == null || subgroups >= 0)
    val counts = if (compact && directItems != null) stringResource(R.string.vault_folder_count, directItems, subgroups ?: 0) else null
    ComponentCard(onOpen, modifier.fillMaxWidth().heightIn(min = if (compact) 104.dp else 0.dp)
        .semantics { if (counts != null) stateDescription = counts }, ModuleAccent.Vault, selected, enabled,
        padding = if (compact) 10.dp else VaurivoLayout.cardPadding,
        spacing = if (compact) 4.dp else VaurivoSpacing.small) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GroupIcon(category, size = if (compact) 40.dp else VaurivoLayout.minimumTouchTarget)
            Spacer(Modifier.weight(1f))
            if (compact && directItems != null) {
                Text(directItems.toString(), Modifier.clearAndSetSemantics {},
                    style = MaterialTheme.typography.bodySmall, color = LocalVaurivoColors.current.textSecondary)
            }
            IconAction(stringResource(R.string.component_menu_for, category.name), Icons.Default.MoreVert, onMenu, enabled = enabled, module = ModuleAccent.Vault)
        }
        Text(category.name, Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
        if (!compact && directItems != null) Text(pluralStringResource(R.plurals.component_direct_items, directItems, directItems),
            Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = LocalVaurivoColors.current.textSecondary)
        if (subgroups != null && (!compact || subgroups > 0)) Text(pluralStringResource(R.plurals.component_subgroups, subgroups, subgroups),
            Modifier.fillMaxWidth().then(if (compact) Modifier.clearAndSetSemantics {} else Modifier), style = MaterialTheme.typography.bodySmall, color = LocalVaurivoColors.current.textSecondary)
    }
}

@Composable
fun VaultItemCard(title: String, category: String, favorite: Boolean, active: Boolean,
                  onOpen: () -> Unit, onFavorite: () -> Unit, modifier: Modifier = Modifier,
                  modifiedLabel: String? = null, selected: Boolean = false, enabled: Boolean = true, compact: Boolean = false) {
    if (!active) return
    val favoriteState = stringResource(if (favorite) R.string.component_favorite else R.string.component_not_favorite)
    ComponentCard(onOpen, modifier.fillMaxWidth(), ModuleAccent.Vault, selected, enabled,
        padding = if (compact) 4.dp else VaurivoLayout.cardPadding) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.extraSmall)) {
                Text(title, Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
                Text(category, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = LocalVaurivoColors.current.textSecondary)
            }
            IconAction(stringResource(R.string.vault_favorite), if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
                onFavorite, Modifier.semantics { stateDescription = favoriteState }, enabled, ModuleAccent.Vault)
        }
        if (modifiedLabel != null) Text(modifiedLabel, Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Content), color = LocalVaurivoColors.current.textTertiary)
    }
}

/** Callbacks must be supplied by an authenticated owner using its existing clipboard/URL services. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SecureFieldCard(label: String, value: String, sensitive: Boolean, fieldType: FieldType,
                    sessionKey: Any, active: Boolean, modifier: Modifier = Modifier,
                    enabled: Boolean = true, onCopy: ((String) -> Unit)? = null,
                    onOpen: ((String) -> Unit)? = null, technical: Boolean = false) {
    val binding = rememberDisplaySession(sessionKey, active)
    if (!active) return
    val secret = sensitive || fieldType == FieldType.SECRET
    val isUrl = fieldType == FieldType.URL
    var revealed by remember(sessionKey, value, secret, fieldType) { mutableStateOf(false) }
    val hidden = stringResource(R.string.component_hidden)
    val shown = stringResource(R.string.component_shown)
    val p = LocalVaurivoColors.current
    Surface(modifier.fillMaxWidth().heightIn(min = 72.dp), color = p.surface,
        shape = VaurivoShapes.card, border = BorderStroke(VaurivoStroke.border, p.borderSubtle)) {
        Column(Modifier.padding(VaurivoLayout.cardPadding), verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
            val safeSemantics = if (secret) Modifier.clearAndSetSemantics {
                contentDescription = "$label, ${if (revealed) shown else hidden}"
                password()
            } else Modifier
            val fieldContent: @Composable (Modifier) -> Unit = { contentModifier ->
                Column(contentModifier, verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.extraSmall)) {
                    Text(label, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                    Text(if (secret && !revealed) "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022" else value,
                        Modifier.fillMaxWidth().then(safeSemantics), style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = if (secret && revealed) FontFamily.Monospace else MaterialTheme.typography.bodyLarge.fontFamily,
                            textDirection = if (technical || secret || isUrl) TextDirection.Ltr else TextDirection.Content), color = p.textPrimary)
                }
            }
            val commands: @Composable () -> Unit = {
              FlowRow(horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
                if (secret) IconAction(stringResource(if (revealed) R.string.component_hide else R.string.component_show, label),
                    if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    { if (binding.matches(sessionKey)) revealed = !revealed }, enabled = enabled, module = ModuleAccent.Vault)
                if (onCopy != null) IconAction(stringResource(R.string.component_copy_field, label), Icons.Default.ContentCopy,
                    { if (binding.matches(sessionKey)) onCopy(value) }, enabled = enabled, module = ModuleAccent.Vault)
                // Keep the exact Phase 02 whitelist; reveal never changes classification.
                if (isUrl && !secret && VaultDocument.validUrl(value) && onOpen != null)
                    IconAction(stringResource(R.string.component_open_field, label), Icons.AutoMirrored.Filled.OpenInNew, {
                        if (binding.matches(sessionKey) && isUrl && !secret && VaultDocument.validUrl(value)) onOpen(value)
                    }, enabled = enabled, module = ModuleAccent.Vault)
              }
            }
            BoxWithConstraints {
                if (maxWidth >= 280.dp && LocalDensity.current.fontScale <= 1.3f) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
                        fieldContent(Modifier.weight(1f))
                        commands()
                    }
                } else Column(verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
                    fieldContent(Modifier.fillMaxWidth())
                    commands()
                }
            }
        }
    }
}

@Composable
fun SecurityIndicator(label: String, modifier: Modifier = Modifier, score: Int? = null) {
    require(score == null || score in 0..4)
    val p = LocalVaurivoColors.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
        Row(horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
            Icon(Icons.Default.Shield, null, tint = p.vaultAccent)
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
        }
        if (score != null) {
            Text(stringResource(R.string.vault_strength, score), Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
            LinearProgressIndicator(progress = { score / 4f }, modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                color = if (score < 2) p.warning else p.vaultAccent, trackColor = p.surfaceRaised)
        }
    }
}
