package com.example.shine.feature.dm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shine.core.designsystem.component.ShineTopAppBar
import com.example.shine.core.designsystem.theme.ShineTheme
import com.example.shine.core.designsystem.util.TimeFormatter
import com.example.shine.core.model.DirectMessage

@Composable
fun DirectMessageDetailRoute(
    dmId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    viewModel: DirectMessageDetailViewModel = hiltViewModel<DirectMessageDetailViewModel, DirectMessageDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(dmId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DirectMessageDetailScreen(
        uiState = uiState,
        onBack = onBack,
        modifier = modifier,
        onNavigateToSettings = onNavigateToSettings,
    )
}

@Composable
fun DirectMessageDetailScreen(
    uiState: DirectMessageDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ShineTopAppBar(
                title = when (uiState) {
                    is DirectMessageDetailUiState.Success -> uiState.directMessage.name
                    else -> "Direct message info"
                },
                onBack = onBack,
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Conversation Settings",
                        )
                    }
                },
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
                DirectMessageDetailUiState.Loading -> CircularProgressIndicator()
                is DirectMessageDetailUiState.Success -> DirectMessageInfo(uiState.directMessage)
                is DirectMessageDetailUiState.Error -> Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        uiState.message.asString(),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectMessageInfo(directMessage: DirectMessage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        InfoRow("Name", directMessage.name)
        directMessage.type?.let { InfoRow("Type", it.replaceFirstChar { c -> c.uppercase() }) }
        directMessage.time?.let { InfoRow("Last message", TimeFormatter.formatDateTime(it)) }
        directMessage.lastActivityAt?.takeIf { it != directMessage.time }?.let {
            InfoRow("Last activity", TimeFormatter.formatDateTime(it))
        }
        directMessage.createdAt?.let { InfoRow("Created", TimeFormatter.formatDateTime(it)) }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    ListItem(
        overlineContent = { Text(label) },
        headlineContent = { Text(value) },
    )
    HorizontalDivider()
}

@Preview(showBackground = true)
@Composable
private fun DirectMessageDetailScreenPreview() {
    ShineTheme(dynamicColor = false) {
        DirectMessageDetailScreen(
            uiState = DirectMessageDetailUiState.Success(
                directMessage = DirectMessage(
                    id = "1",
                    name = "Alice Smith",
                    time = 1_779_780_643L,
                    type = "direct",
                    createdAt = 1_779_700_000L,
                    lastActivityAt = 1_779_780_643L,
                ),
            ),
            onBack = {},
        )
    }
}
