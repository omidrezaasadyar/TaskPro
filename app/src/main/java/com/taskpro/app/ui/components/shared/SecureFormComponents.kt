package com.taskpro.app.ui.components.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextDirection
import com.taskpro.app.R
import com.taskpro.app.ui.theme.*
import com.taskpro.app.vault.FieldType

internal fun secureKeyboardType(type: FieldType, sensitive: Boolean, override: KeyboardType?): KeyboardType = when {
    (sensitive || type == FieldType.SECRET) && override == KeyboardType.NumberPassword -> KeyboardType.NumberPassword
    sensitive || type == FieldType.SECRET -> KeyboardType.Password
    override != null -> override
    type == FieldType.URL -> KeyboardType.Uri
    type == FieldType.EMAIL -> KeyboardType.Email
    type == FieldType.NUMBER -> KeyboardType.Decimal
    else -> KeyboardType.Text
}

/** Values and lifetime belong to the authenticated owner. Nothing is saveable here. */
@Composable
fun SecureInputField(label: String, value: String, onValueChange: (String) -> Unit,
                     sessionKey: Any, active: Boolean, modifier: Modifier = Modifier,
                     type: FieldType = FieldType.TEXT, sensitive: Boolean = false,
                     enabled: Boolean = true, busy: Boolean = false, error: String? = null, readOnly: Boolean = false,
                     keyboardType: KeyboardType? = null) {
    val binding = rememberDisplaySession(sessionKey, active)
    if (!active) return
    key(sessionKey) {
        val secret = sensitive || type == FieldType.SECRET
        var revealed by remember(secret, type) { mutableStateOf(false) }
        var hasFocus by remember { mutableStateOf(false) }
        var editor by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
        if (editor.text != value) editor = TextFieldValue(value, TextRange(value.length))
        val focus = remember { FocusRequester() }
        val hidden = stringResource(R.string.component_hidden)
        val shown = stringResource(R.string.component_shown)
        val available = enabled && !busy
        val update: (TextFieldValue) -> Boolean = {
            if (available && !readOnly && binding.matches(sessionKey)) {
                editor = it
                onValueChange(it.text)
                true
            } else false
        }
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.extraSmall)) {
            // Remove raw text/layout/copy semantics even when deliberately revealed.
            // SetText and focus remain explicit accessible commands; reveal is a sibling target.
            val redacted = if (secret) Modifier.clearAndSetSemantics {
                contentDescription = "$label, ${if (revealed) shown else hidden}"
                password()
                focused = hasFocus
                isEditable = available && !readOnly
                editableText = AnnotatedString(if (value.isEmpty()) "" else "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022")
                if (!available) disabled() else {
                    if (!readOnly) {
                        setText { update(TextFieldValue(it.text, TextRange(it.length))) }
                        insertTextAtCursor { input ->
                            val start = editor.selection.min
                            val next = editor.text.replaceRange(start, editor.selection.max, input.text)
                            update(TextFieldValue(next, TextRange(start + input.length)))
                        }
                    }
                    requestFocus { if (binding.matches(sessionKey)) { focus.requestFocus(); true } else false }
                    onClick { if (binding.matches(sessionKey)) { focus.requestFocus(); true } else false }
                }
            } else Modifier
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(editor, { update(it) }, enabled = available, readOnly = readOnly, label = { Text(label) },
                modifier = Modifier.weight(1f).heightIn(min = VaurivoLayout.primaryActionHeight).then(redacted)
                    .onFocusChanged { hasFocus = it.isFocused }.focusRequester(focus),
                shape = VaurivoShapes.control, isError = error != null, singleLine = type != FieldType.NOTES,
                minLines = if (type == FieldType.NOTES) 3 else 1,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = if (secret && revealed) FontFamily.Monospace else MaterialTheme.typography.bodyLarge.fontFamily,
                    textDirection = when {
                    secret || type in listOf(FieldType.URL, FieldType.EMAIL, FieldType.NUMBER, FieldType.DATE) -> TextDirection.Ltr
                    else -> TextDirection.Content
                }),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = secureKeyboardType(type, sensitive, keyboardType)),
                visualTransformation = if (secret && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
                leadingIcon = if (secret) ({ Icon(Icons.Default.Lock, null) }) else null,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ModuleAccent.Vault.color(),
                    focusedLabelColor = ModuleAccent.Vault.color(), cursorColor = ModuleAccent.Vault.color(),
                    unfocusedBorderColor = LocalVaurivoColors.current.outlineInteractive))
            if (secret) IconAction(stringResource(if (revealed) R.string.component_hide else R.string.component_show, label),
                if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility, { if (binding.matches(sessionKey)) revealed = !revealed },
                enabled = available, module = ModuleAccent.Vault)
            }
            if (error != null) ErrorState(error)
        }
    }
}

@Immutable
data class GeneratorOptions(val length: Int = 24, val lower: Boolean = true, val upper: Boolean = true,
                            val digits: Boolean = true, val symbols: Boolean = true) {
    init { require(length in 12..128) }
    val valid get() = lower || upper || digits || symbols
}

class GeneratedPasswordCandidate(val value: String, val options: GeneratorOptions, val sessionKey: Any) {
    override fun toString() = "GeneratedPasswordCandidate(redacted)"
}

/** No RNG or form mutation: generate and apply are separate, owner-authorized callbacks. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PasswordGeneratorPanel(options: GeneratorOptions, candidate: GeneratedPasswordCandidate?,
                           onOptionsChange: (GeneratorOptions) -> Unit, onGenerate: () -> Unit,
                           onUsePassword: (String) -> Unit, onCancel: () -> Unit, sessionKey: Any, active: Boolean,
                           modifier: Modifier = Modifier, busy: Boolean = false) {
    val binding = rememberDisplaySession(sessionKey, active)
    if (!active) return
    val p = LocalVaurivoColors.current
    val lengthLabel = stringResource(R.string.vault_length)
    val currentCandidate = candidate?.takeIf { it.options == options && it.sessionKey == sessionKey && it.value.length == options.length }
    val updateOptions: (GeneratorOptions) -> Unit = { if (!busy && binding.matches(sessionKey)) onOptionsChange(it) }
    Surface(modifier.fillMaxWidth(), shape = VaurivoShapes.card, color = p.surface,
        border = BorderStroke(VaurivoStroke.border, p.outlineInteractive)) {
        Column(Modifier.padding(VaurivoLayout.cardPadding), verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
            Text(stringResource(R.string.vault_generate), Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
            Text("${stringResource(R.string.vault_length)}: ${options.length}", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium)
            Slider(options.length.toFloat(), { updateOptions(options.copy(length = it.toInt().coerceIn(12, 128))) },
                enabled = !busy, valueRange = 12f..128f, steps = 115,
                modifier = Modifier.fillMaxWidth().heightIn(min = VaurivoLayout.minimumTouchTarget)
                    .semantics { contentDescription = lengthLabel },
                colors = SliderDefaults.colors(thumbColor = p.vaultAccent, activeTrackColor = p.vaultAccent,
                    inactiveTrackColor = p.surfaceRaised, activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent))
            Row {
                IconAction(stringResource(R.string.component_decrease), Icons.Default.Remove,
                    { updateOptions(options.copy(length = options.length - 1)) }, enabled = !busy && options.length > 12, module = ModuleAccent.Vault)
                IconAction(stringResource(R.string.component_increase), Icons.Default.Add,
                    { updateOptions(options.copy(length = options.length + 1)) }, enabled = !busy && options.length < 128, module = ModuleAccent.Vault)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.small), verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
                GeneratorToggle(stringResource(R.string.vault_lower), options.lower, !busy) { updateOptions(options.copy(lower = it)) }
                GeneratorToggle(stringResource(R.string.vault_upper), options.upper, !busy) { updateOptions(options.copy(upper = it)) }
                GeneratorToggle(stringResource(R.string.vault_digits), options.digits, !busy) { updateOptions(options.copy(digits = it)) }
                GeneratorToggle(stringResource(R.string.vault_symbols), options.symbols, !busy) { updateOptions(options.copy(symbols = it)) }
            }
            if (!options.valid) ErrorState(stringResource(R.string.component_generator_classes))
            // Only a candidate tied to the current controls/session can be presented/applied.
            if (currentCandidate != null) {
                key(sessionKey, currentCandidate) {
                    SecureInputField(stringResource(R.string.component_candidate), currentCandidate.value, {}, sessionKey, active,
                        type = FieldType.SECRET, readOnly = true, enabled = !busy)
                }
            }
            OutlinedButton({ if (binding.matches(sessionKey)) onGenerate() }, enabled = options.valid && !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = VaurivoLayout.primaryActionHeight), shape = VaurivoShapes.control,
                border = BorderStroke(VaurivoStroke.border, p.outlineInteractive),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = p.vaultAccent)) {
                Icon(if (busy) Icons.Default.HourglassEmpty else Icons.Default.Refresh, null)
                Spacer(Modifier.width(VaurivoSpacing.small))
                Text(stringResource(R.string.vault_generate), Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            PrimaryActionButton(stringResource(R.string.component_use_password), {
                if (currentCandidate != null && options.valid && binding.matches(sessionKey)) onUsePassword(currentCandidate.value)
            }, module = ModuleAccent.Vault, icon = Icons.Default.Check,
                enabled = currentCandidate != null && options.valid, busy = busy)
            TextButton({ if (binding.matches(sessionKey)) onCancel() }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = VaurivoLayout.minimumTouchTarget),
                colors = ButtonDefaults.textButtonColors(contentColor = p.textSecondary)) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun GeneratorToggle(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    FilterChip(label, checked, { onChange(!checked) }, modifier = Modifier.semantics { contentDescription = label },
        module = ModuleAccent.Vault, enabled = enabled)
}
