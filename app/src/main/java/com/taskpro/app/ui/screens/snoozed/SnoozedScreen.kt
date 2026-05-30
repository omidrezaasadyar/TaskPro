package com.taskpro.app.ui.screens.snoozed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskpro.app.R
import com.taskpro.app.ui.AppViewModelProvider
import com.taskpro.app.ui.components.TaskCard
import com.taskpro.app.ui.theme.StatusCompleted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnoozedScreen(
    onBack: () -> Unit,
    viewModel: SnoozedViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.snoozed)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.empty_snoozed),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(task = task) {
                        Row {
                            IconButton(onClick = { viewModel.complete(task) }) {
                                Icon(
                                    Icons.Default.Check,
                                    stringResource(R.string.mark_complete),
                                    tint = StatusCompleted
                                )
                            }
                            IconButton(onClick = { viewModel.resume(task) }) {
                                Icon(Icons.Default.Undo, stringResource(R.string.resume))
                            }
                            IconButton(onClick = { viewModel.delete(task) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
