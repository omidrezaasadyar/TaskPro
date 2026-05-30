package com.taskpro.app.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskpro.app.R
import com.taskpro.app.data.TaskStatus
import com.taskpro.app.ui.AppViewModelProvider
import com.taskpro.app.ui.components.GradientBackground
import com.taskpro.app.ui.components.TaskCard
import com.taskpro.app.ui.theme.StatusCompleted
import com.taskpro.app.ui.theme.StatusPending
import com.taskpro.app.ui.theme.StatusSnoozed

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onResultClick: (Long) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        GradientBackground {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Default.Clear, stringResource(R.string.cancel))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )

                when {
                    query.isBlank() -> SearchPlaceholder(
                        illustration = R.drawable.illustration_search,
                        title = stringResource(R.string.search_empty_title),
                        subtitle = stringResource(R.string.search_empty_subtitle)
                    )

                    results.isEmpty() -> SearchPlaceholder(
                        illustration = R.drawable.illustration_search,
                        title = stringResource(R.string.search_no_results, query),
                        subtitle = ""
                    )

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(results, key = { it.task.id }) { row ->
                            TaskCard(
                                task = row.task,
                                trailing = {
                                    StatusDot(row.task.status)
                                    Spacer(Modifier.size(8.dp))
                                }
                            )
                            Text(
                                text = stringResource(R.string.in_item, row.itemName),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDot(status: TaskStatus) {
    val color = when (status) {
        TaskStatus.PENDING -> StatusPending
        TaskStatus.COMPLETED -> StatusCompleted
        TaskStatus.SNOOZED -> StatusSnoozed
    }
    Box(
        Modifier
            .padding(end = 4.dp)
            .size(10.dp)
            .clipCircle(color)
    )
}

private fun Modifier.clipCircle(color: Color) = this
    .then(androidx.compose.foundation.background(color, androidx.compose.foundation.shape.CircleShape))

@Composable
private fun SearchPlaceholder(illustration: Int, title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                painter = painterResource(illustration),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(160.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
