package com.example.shine.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shine.domain.model.Channel
import com.example.shine.ui.home.ChannelUiState
import com.example.shine.ui.home.ChannelViewModel
import com.example.shine.ui.theme.ShineTheme

@Composable
fun ChannelsRoute(
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChannelViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChannelsContent(
        uiState = uiState,
        onChannelClick = onChannelClick,
        modifier = modifier,
    )
}

@Composable
fun ChannelsContent(
    uiState: ChannelUiState,
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading && uiState.channels.isEmpty() -> CircularProgressIndicator()
            uiState.errorMessage != null && uiState.channels.isEmpty() -> Text(
                text = uiState.errorMessage.asString(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp),
            )
            uiState.channels.isEmpty() -> Text("No channels yet")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.channels, key = { it.id }) { channel ->
                    ChannelItem(channel = channel, onClick = { onChannelClick(channel) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ChannelItem(channel: Channel, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = if (channel.isPrivate) Icons.Filled.Lock else Icons.Filled.Tag,
                contentDescription = if (channel.isPrivate) "Private channel" else "Public channel",
            )
        },
        headlineContent = { Text(channel.name) },
        supportingContent = channel.categoryName?.let { category -> { Text(category) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun ChannelsContentPreview() {
    ShineTheme(dynamicColor = false) {
        ChannelsContent(
            uiState = ChannelUiState(
                channels = listOf(
                    Channel("1", "general", false, false, true, null, null, null),
                    Channel("2", "random", false, false, false, null, null, null),
                ),
            ),
            onChannelClick = {},
        )
    }
}

