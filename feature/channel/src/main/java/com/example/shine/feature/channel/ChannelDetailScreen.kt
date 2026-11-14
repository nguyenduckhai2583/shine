package com.example.shine.feature.channel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.example.shine.core.model.Channel
import com.example.shine.core.designsystem.component.ShineTopAppBar
import com.example.shine.core.designsystem.theme.ShineTheme
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
    ChannelDetailScreen(
        uiState = uiState,
        onBack = onBack,
    )
}

@Composable
fun ChannelDetailScreen(
    uiState: ChannelDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ShineTopAppBar(
                title = when (uiState) {
                    is ChannelDetailUiState.Success -> "#${uiState.channel.name}"
                    else -> "Channel info"
                },
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
                ChannelDetailUiState.Loading -> CircularProgressIndicator()
                is ChannelDetailUiState.Success -> ChannelInfo(uiState.channel)
                is ChannelDetailUiState.Error -> Column(
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
            uiState = ChannelDetailUiState.Success(
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
        )
    }
}
