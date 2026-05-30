package com.taskpro.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

/** Simple single-field dialog for creating/renaming an item. */
@Composable
fun NameDialog(
    title: String,
    label: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initialValue: String = ""
) {
    var text by remember {
        mutableStateOf(TextFieldValue(initialValue, selection = androidx.compose.ui.text.TextRange(initialValue.length)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.text) },
                enabled = text.text.isNotBlank()
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = androidx.compose.ui.res.stringResource(com.taskpro.app.R.string.cancel)) }
        }
    )
}
