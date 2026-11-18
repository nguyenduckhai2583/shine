package com.example.shine.feature.planix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.shine.core.designsystem.component.ShineTopAppBar
import com.example.shine.core.designsystem.theme.MultiDevicePreviews
import com.example.shine.core.designsystem.theme.ShineTheme
import com.example.shine.core.model.Project

@Composable
fun PlanixDetailRoute(
    viewModel: PlanixDetailViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PlanixDetailScreen(
        uiState = uiState,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun PlanixDetailScreen(
    uiState: PlanixDetailUiState,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ShineTopAppBar(
                title = stringResource(R.string.project_details),
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                is PlanixDetailUiState.Loading -> {
                    CircularProgressIndicator()
                }

                is PlanixDetailUiState.Error -> {
                    Text(
                        text = uiState.message.asString(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(24.dp),
                    )
                }

                is PlanixDetailUiState.Success -> {
                    PlanixDetailContent(
                        project = uiState.project,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanixDetailContent(
    project: Project,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!project.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = project.logoUrl,
                        contentDescription = project.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = project.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    project.prefix?.let { prefix ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text("[$prefix]") },
                        )
                    }
                    project.status?.let { status ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(status) },
                        )
                    }
                }
            }
        }

        // Details Section Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Leader",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.project_leader)) },
                    supportingContent = {
                        Text(
                            text = project.leaderName ?: stringResource(R.string.unassigned),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = stringResource(R.string.project_key_id),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.project_key_id)) },
                    supportingContent = {
                        Text(
                            text = project.id,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun PlanixDetailPlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Assignment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
            Text(
                text = stringResource(R.string.select_project_to_view_details),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@MultiDevicePreviews
@Composable
private fun PlanixDetailScreenSuccessPreview() {
    ShineTheme(dynamicColor = false) {
        PlanixDetailScreen(
            uiState = PlanixDetailUiState.Success(
                Project(
                    id = "proj-1",
                    name = "Mobile App Redesign",
                    prefix = "MAR",
                    status = "ACTIVE",
                    logoUrl = null,
                    leaderName = "John Doe",
                )
            ),
            onBack = {},
        )
    }
}

@MultiDevicePreviews
@Composable
private fun PlanixDetailPlaceholderPreview() {
    ShineTheme(dynamicColor = false) {
        PlanixDetailPlaceholder()
    }
}
