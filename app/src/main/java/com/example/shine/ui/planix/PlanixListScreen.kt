package com.example.shine.ui.planix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shine.domain.model.Project
import com.example.shine.ui.theme.ShineTheme

@Composable
fun PlanixListRoute(
    onBack: () -> Unit,
    viewModel: PlanixListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PlanixListScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::loadProjects,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanixListScreen(
    uiState: PlanixListUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Planix Projects") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search projects...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    uiState.isLoading && uiState.projects.isEmpty() -> CircularProgressIndicator()
                    uiState.errorMessage != null && uiState.projects.isEmpty() -> Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(uiState.errorMessage.asString(), color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = onRetry) { Text("Retry") }
                    }
                    uiState.projects.isEmpty() -> Text("No projects found")
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.projects, key = { it.id }) { project ->
                            ProjectItem(project = project)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectItem(project: Project) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = "Project",
                )
            },
            headlineContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium)
                    project.prefix?.let { prefix ->
                        Text(
                            text = "[$prefix]",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    project.leaderName?.let { leader ->
                        Text(
                            text = "Leader: $leader",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    project.status?.let { status ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(status) },
                        )
                    }
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlanixListScreenPreview() {
    ShineTheme(dynamicColor = false) {
        PlanixListScreen(
            uiState = PlanixListUiState(
                projects = listOf(
                    Project("1", "Mobile App Redesign", "MAR", "ACTIVE", null, "John Doe"),
                    Project("2", "Backend Migration", "BM", "ACTIVE", null, "Alice Smith"),
                ),
            ),
            onBack = {},
            onRetry = {},
            onSearchQueryChange = {},
        )
    }
}
