package com.example.shine.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tag
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

@Composable
fun HomeRoute(
    onChannelClick: (Channel) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onChannelClick = onChannelClick,
        onRetry = viewModel::loadChannels,
        onSignOut = viewModel::signOut,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onChannelClick: (Channel) -> Unit,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Channels")
                        if (uiState.displayName.isNotEmpty()) {
                            Text(uiState.displayName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
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
            when {
                uiState.isLoading && uiState.channels.isEmpty() -> CircularProgressIndicator()
                uiState.errorMessage != null && uiState.channels.isEmpty() -> Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRetry) { Text("Retry") }
                }
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
private fun HomeScreenPreview() {
    ShineTheme(dynamicColor = false) {
        HomeScreen(
            uiState = HomeUiState(
                displayName = "Jane Doe",
                channels = listOf(
                    Channel("1", "general", false, false, true, null, null, null),
                    Channel("2", "devops-support", true, false, false, "diginex", null, null),
                ),
            ),
            onChannelClick = {},
            onRetry = {},
            onSignOut = {},
        )
    }
}
