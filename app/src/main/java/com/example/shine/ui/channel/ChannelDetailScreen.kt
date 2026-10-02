package com.example.shine.ui.channel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.example.shine.domain.model.Channel
import com.example.shine.ui.theme.ShineTheme
import java.text.DateFormat
import java.util.Date

@Composable
fun ChannelDetailRoute(
    channelId: String,
    onBack: () -> Unit,
    viewModel: ChannelDetailViewModel = hiltViewModel<ChannelDetailViewModel, ChannelDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(channelId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChannelDetailScreen(uiState = uiState, onBack = onBack, onRetry = viewModel::load)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelDetailScreen(
    uiState: ChannelDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.channel?.let { "#${it.name}" } ?: "Channel info") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            val channel = uiState.channel
            when {
                channel != null -> ChannelInfo(channel)
                uiState.isLoading -> CircularProgressIndicator()
                else -> Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        uiState.errorMessage ?: "Something went wrong",
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(onClick = onRetry) { Text("Retry") }
                }
            }
        }
    }
}

@Composable
private fun ChannelInfo(channel: Channel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        InfoRow("Name", channel.name)
        InfoRow("Visibility", if (channel.isPrivate) "Private" else "Public")
        channel.categoryName?.let { InfoRow("Category", it) }
        InfoRow("Encrypted", if (channel.isEncrypted) "Yes" else "No")
        if (channel.isDefault) InfoRow("Default channel", "Yes")
        channel.createdAt?.let { InfoRow("Created", formatEpochSeconds(it)) }
        channel.lastActivityAt?.let { InfoRow("Last activity", formatEpochSeconds(it)) }
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

private fun formatEpochSeconds(seconds: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(seconds * 1000))

@Preview(showBackground = true)
@Composable
private fun ChannelDetailScreenPreview() {
    ShineTheme(dynamicColor = false) {
        ChannelDetailScreen(
            uiState = ChannelDetailUiState(
                channel = Channel(
                    id = "1",
                    name = "devops-support",
                    isPrivate = true,
                    isEncrypted = false,
                    isDefault = false,
                    categoryName = "diginex",
                    createdAt = 1_779_780_643,
                    lastActivityAt = 1_790_753_979,
                ),
            ),
            onBack = {},
            onRetry = {},
        )
    }
}
