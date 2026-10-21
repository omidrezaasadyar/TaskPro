package com.taskpro.app.ui.components.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.taskpro.app.ui.theme.*

// Presentation identity only; never persisted or used as an authentication decision.
enum class ModuleAccent { Tasks, Vault }

// Guards stale UI events only. The owner still authenticates and authorizes every operation.
internal class DisplaySession(var key: Any?) {
    fun matches(expected: Any) = key == expected
}

@Composable
internal fun rememberDisplaySession(key: Any, active: Boolean): DisplaySession {
    val binding = remember { DisplaySession(null) }
    binding.key = if (active) key else null
    DisposableEffect(binding) { onDispose { binding.key = null } }
    return binding
}

@Composable internal fun ModuleAccent.color(): Color = with(LocalVaurivoColors.current) {
    if (this@color == ModuleAccent.Tasks) tasksAccent else vaultAccent
}
@Composable internal fun ModuleAccent.container(): Color = with(LocalVaurivoColors.current) {
    if (this@container == ModuleAccent.Tasks) tasksContainer else vaultContainer
}

@Composable
internal fun Modifier.componentFocus(source: MutableInteractionSource, module: ModuleAccent, shape: Shape): Modifier {
    val focused by source.collectIsFocusedAsState()
    val palette = LocalVaurivoColors.current
    val color = if (!focused) Color.Transparent else if (module == ModuleAccent.Tasks) palette.focusTasks else palette.focusVault
    return border(VaurivoStroke.focus, color, shape)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier,
               enabled: Boolean = true, module: ModuleAccent = ModuleAccent.Tasks) {
    val source = remember { MutableInteractionSource() }
    TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } }, state = rememberTooltipState()) {
        IconButton(onClick, enabled = enabled, interactionSource = source,
            modifier = modifier.size(VaurivoLayout.minimumTouchTarget).componentFocus(source, module, VaurivoShapes.utility)) {
            Icon(icon, label, tint = if (enabled) LocalVaurivoColors.current.textSecondary else LocalVaurivoColors.current.disabledContent,
                modifier = Modifier.size(VaurivoLayout.iconSize))
        }
    }
}

@Composable
fun ModuleAccentTile(icon: ImageVector, module: ModuleAccent, modifier: Modifier = Modifier) {
    Surface(modifier.size(40.dp), shape = VaurivoShapes.control, color = module.container(), contentColor = module.color()) {
        Box(contentAlignment = androidx.compose.ui.Alignment.Center) { Icon(icon, null, Modifier.size(VaurivoLayout.iconSize)) }
    }
}

@Composable
internal fun ComponentCard(onClick: () -> Unit, modifier: Modifier, module: ModuleAccent,
                           selected: Boolean = false, enabled: Boolean = true,
                           padding: androidx.compose.ui.unit.Dp = VaurivoLayout.cardPadding,
                           spacing: androidx.compose.ui.unit.Dp = VaurivoSpacing.small,
                           content: @Composable ColumnScope.() -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()
    val p = LocalVaurivoColors.current
    Surface(onClick, enabled = enabled, interactionSource = source,
        modifier = modifier.componentFocus(source, module, VaurivoShapes.card).semantics {
            role = Role.Button
            this.selected = selected
        }, shape = VaurivoShapes.card,
        color = if (selected) module.container() else if (pressed || hovered) p.surfaceRaised else p.surface,
        contentColor = if (enabled) p.textPrimary else p.disabledContent,
        border = BorderStroke(VaurivoStroke.border, if (selected) module.color() else p.borderSubtle),
        shadowElevation = VaurivoElevation.card) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
    }
}
