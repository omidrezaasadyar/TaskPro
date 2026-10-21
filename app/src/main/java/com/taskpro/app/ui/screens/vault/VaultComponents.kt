package com.taskpro.app.ui.screens.vault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.taskpro.app.R
import com.taskpro.app.vault.*
import com.taskpro.app.ui.screens.vaultAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun Modifier.vaultActivity(): Modifier {
    val app = LocalContext.current.applicationContext as? com.taskpro.app.TaskProApp
    return onPreviewKeyEvent { app?.container?.vaultRepository?.touch(); false }.pointerInput(app) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial)
                app?.container?.vaultRepository?.touch()
            }
        }
    }
}

@Composable
fun VaultInput(label: String, value: String, change: (String) -> Unit, secret: Boolean = false,
               type: KeyboardType = KeyboardType.Text, multiline: Boolean = false, modifier: Modifier = Modifier) {
    var revealed by remember { mutableStateOf(false) }
    val app = LocalContext.current.applicationContext as? com.taskpro.app.TaskProApp
    OutlinedTextField(
        value = value, onValueChange = { app?.container?.vaultRepository?.touch(); change(it) }, label = { Text(label) },
        modifier = modifier.fillMaxWidth().then(if (secret) Modifier.semantics { password() } else Modifier),
        singleLine = !multiline, minLines = if (multiline) 3 else 1,
        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
        keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else type, autoCorrectEnabled = false),
        visualTransformation = if (secret && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = vaultAccent(),
            focusedLabelColor = vaultAccent(), cursorColor = vaultAccent()),
        leadingIcon = if (secret) ({ Icon(Icons.Default.Lock, null) }) else null,
        trailingIcon = if (secret) ({ IconButton(onClick = { revealed = !revealed }) {
            Icon(if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility, stringResource(R.string.vault_reveal))
        } }) else null
    )
}

@Composable
fun <T> VaultChoice(label: String, value: T, options: List<Pair<T, String>>, onChoose: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) {
            Text("$label: ${options.firstOrNull { it.first == value }?.second.orEmpty()}", Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 360.dp)) {
            options.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { onChoose(id); open = false }) }
        }
    }
}

@Composable
fun VaultToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = checked,
        role = androidx.compose.ui.semantics.Role.Checkbox, onValueChange = onChange), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Checkbox(checked, onCheckedChange = null)
        Text(label, Modifier.weight(1f))
    }
}

fun categoryIcon(icon: CategoryIcon): ImageVector = when (icon) {
    CategoryIcon.FOLDER -> Icons.Default.Folder
    CategoryIcon.BANK -> Icons.Default.AccountBalance
    CategoryIcon.EMAIL -> Icons.Default.Email
    CategoryIcon.WEB -> Icons.Default.Language
    CategoryIcon.BUSINESS -> Icons.Default.Business
    CategoryIcon.SERVER -> Icons.Default.Dns
    CategoryIcon.KEY -> Icons.Default.Key
    CategoryIcon.PERSON -> Icons.Default.Person
}
fun categoryLabel(icon: CategoryIcon): Int = when (icon) {
    CategoryIcon.FOLDER -> R.string.vault_folder
    CategoryIcon.BANK -> R.string.vault_bank
    CategoryIcon.EMAIL -> R.string.vault_email
    CategoryIcon.WEB -> R.string.vault_web
    CategoryIcon.BUSINESS -> R.string.vault_business
    CategoryIcon.SERVER -> R.string.vault_server
    CategoryIcon.KEY -> R.string.vault_key
    CategoryIcon.PERSON -> R.string.vault_person
}
fun fieldLabel(field: String): Int = when (field) {
    "url" -> R.string.vault_url; "username" -> R.string.vault_username; "email" -> R.string.vault_email
    "password" -> R.string.vault_password; "phone" -> R.string.vault_phone; "reference" -> R.string.vault_reference
    else -> R.string.vault_notes
}
fun typeLabel(type: FieldType): Int = when (type) {
    FieldType.TEXT -> R.string.vault_text; FieldType.SECRET -> R.string.vault_secret; FieldType.URL -> R.string.vault_url
    FieldType.EMAIL -> R.string.vault_email; FieldType.NUMBER -> R.string.vault_number
    FieldType.DATE -> R.string.vault_date; FieldType.NOTES -> R.string.vault_notes
}
fun categoryPath(id: String?, categories: List<VaultCategory>): String {
    val names = mutableListOf<String>()
    var current = id
    val visited = mutableSetOf<String>()
    val byId = categories.associateBy { it.id }
    while (current != null && visited.add(current)) {
        val category = byId[current] ?: break
        names.add(category.name); current = category.parentId
    }
    return names.reversed().joinToString(" / ")
}
fun descendants(id: String, categories: List<VaultCategory>): Set<String> {
    val result = mutableSetOf(id)
    var changed: Boolean
    do { changed = false; categories.forEach { if (it.parentId in result && result.add(it.id)) changed = true } } while (changed)
    return result
}

@Composable
fun PasswordStrength(value: String) {
    var score by remember { mutableIntStateOf(0) }
    LaunchedEffect(value) {
        delay(300)
        score = withContext(Dispatchers.Default) { com.nulabinc.zxcvbn.Zxcvbn().measure(value.take(128)).score }
    }
    if (value.isNotBlank()) Text(stringResource(R.string.vault_strength, score), style = MaterialTheme.typography.bodySmall)
}

@Composable
fun PasswordGenerator(onDismiss: () -> Unit, onGenerate: (String) -> Unit) {
    var length by remember { mutableFloatStateOf(24f) }
    var lower by remember { mutableStateOf(true) }; var upper by remember { mutableStateOf(true) }
    var digits by remember { mutableStateOf(true) }; var symbols by remember { mutableStateOf(true) }
    AlertDialog(onDismissRequest = onDismiss, properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
        title = { Text(stringResource(R.string.vault_generate)) }, text = {
            Column(Modifier.vaultActivity().verticalScroll(rememberScrollState())) {
                Text("${stringResource(R.string.vault_length)}: ${length.toInt()}")
                Slider(length, { length = it }, valueRange = 12f..128f, steps = 115)
                VaultToggle(stringResource(R.string.vault_lower), lower) { lower = it }
                VaultToggle(stringResource(R.string.vault_upper), upper) { upper = it }
                VaultToggle(stringResource(R.string.vault_digits), digits) { digits = it }
                VaultToggle(stringResource(R.string.vault_symbols), symbols) { symbols = it }
            }
        }, confirmButton = { TextButton(enabled = lower || upper || digits || symbols,
            onClick = { onGenerate(VaultCrypto.generate(length.toInt(), lower, upper, digits, symbols)); onDismiss() }) {
            Text(stringResource(R.string.vault_generate))
        } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}
