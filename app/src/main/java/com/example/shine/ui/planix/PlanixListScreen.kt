package com.example.shine.ui.planix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.shine.R
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Project
import com.example.shine.ui.common.UiText
import com.example.shine.ui.common.toUiText
import com.example.shine.ui.theme.ShineTheme
import kotlinx.coroutines.flow.flowOf

@Composable
fun PlanixListRoute(
    onBack: () -> Unit,
    viewModel: PlanixListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val projects = viewModel.projectsPagingFlow.collectAsLazyPagingItems()
    PlanixListScreen(
        uiState = uiState,
        projects = projects,
        onBack = onBack,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanixListScreen(
    uiState: PlanixListUiState,
    projects: LazyPagingItems<Project>,
    onBack: () -> Unit,
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

            val refreshLoadState = projects.loadState.refresh
            val appendLoadState = projects.loadState.append

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                when (refreshLoadState) {
                    is LoadState.Loading if projects.itemCount == 0 -> {
                        CircularProgressIndicator()
                    }

                    is LoadState.Error if projects.itemCount == 0 -> {
                        val error = (refreshLoadState.error as? AppException)?.toUiText()
                            ?: UiText.Resource(R.string.error_unknown)
                        Text(
                            text = error.asString(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(24.dp),
                        )
                    }

                    is LoadState.NotLoading if projects.itemCount == 0 -> {
                        Text("No projects found")
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                count = projects.itemCount,
                                key = projects.itemKey { it.id },
                            ) { index ->
                                val project = projects[index]
                                if (project != null) {
                                    ProjectItem(project = project)
                                }
                            }

                            if (appendLoadState is LoadState.Loading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }

                            if (appendLoadState is LoadState.Error) {
                                item {
                                    val error = (appendLoadState.error as? AppException)?.toUiText()
                                        ?: UiText.Resource(R.string.error_unknown)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(error.asString(), color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
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
                    imageVector = Icons.AutoMirrored.Filled.Assignment,
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
        val fakeProjects = flowOf(
            PagingData.from(
                listOf(
                    Project("1", "Mobile App Redesign", "MAR", "ACTIVE", null, "John Doe"),
                    Project("2", "Backend Migration", "BM", "ACTIVE", null, "Alice Smith"),
                )
            )
        ).collectAsLazyPagingItems()

        PlanixListScreen(
            uiState = PlanixListUiState(),
            projects = fakeProjects,
            onBack = {},
            onSearchQueryChange = {},
        )
    }
}
