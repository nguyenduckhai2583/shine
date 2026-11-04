package com.example.shine.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    var showCreateChannelDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message.asString(context))
            viewModel.clearSnackbarMessage()
        }
    }

    ChannelsContent(
        uiState = uiState,
        onChannelClick = onChannelClick,
        onAddChannelClick = { showCreateChannelDialog = true },
        onRetrySync = viewModel::retrySync,
        onDeleteChannel = viewModel::deleteChannel,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    if (showCreateChannelDialog) {
        CreateChannelDialog(
            onDismiss = { showCreateChannelDialog = false },
            onCreateChannel = { name ->
                viewModel.createChannel(name)
                showCreateChannelDialog = false
            },
        )
    }
}

@Composable
fun ChannelsContent(
    uiState: ChannelUiState,
    onChannelClick: (Channel) -> Unit,
    onAddChannelClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRetrySync: (String) -> Unit = {},
    onDeleteChannel: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var errorChannelToHandle by remember { mutableStateOf<Channel?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            uiState.isLoading && uiState.channels.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null && uiState.channels.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.errorMessage.asString(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }

            uiState.channels.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tag,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "No channels yet",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Channels are where conversations happen. Tap + to create a public channel.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                items(uiState.channels, key = { it.id }) { channel ->
                    ChannelItem(
                        channel = channel,
                        onClick = {
                            when (channel.syncStatus) {
                                com.example.shine.domain.model.SyncStatus.SYNCED -> onChannelClick(channel)
                                com.example.shine.domain.model.SyncStatus.ERROR -> errorChannelToHandle = channel
                                com.example.shine.domain.model.SyncStatus.PENDING -> { /* Disabled */ }
                            }
                        },
                        modifier = Modifier.animateItem(),
                    )
                    HorizontalDivider(modifier = Modifier.animateItem())
                }
            }
        }

        if (!uiState.isLoading) {
            FloatingActionButton(
                onClick = onAddChannelClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create channel",
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )

        errorChannelToHandle?.let { channel ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { errorChannelToHandle = null },
                title = { Text(androidx.compose.ui.res.stringResource(com.example.shine.R.string.channel_error_dialog_title)) },
                text = {
                    Text(
                        channel.syncErrorMessage
                            ?: androidx.compose.ui.res.stringResource(
                                com.example.shine.R.string.channel_error_dialog_default_message,
                                channel.name,
                            )
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            onRetrySync(channel.id)
                            errorChannelToHandle = null
                        }
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.example.shine.R.string.action_retry))
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            onDeleteChannel(channel.id)
                            errorChannelToHandle = null
                        }
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.example.shine.R.string.action_delete))
                    }
                }
            )
        }
    }
}

@Composable
private fun ChannelItem(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPending = channel.syncStatus == com.example.shine.domain.model.SyncStatus.PENDING
    val isError = channel.syncStatus == com.example.shine.domain.model.SyncStatus.ERROR
    val isEnabled = channel.syncStatus == com.example.shine.domain.model.SyncStatus.SYNCED || isError

    ListItem(
        modifier = modifier.clickable(enabled = isEnabled, onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = if (channel.isPrivate) Icons.Filled.Lock else Icons.Filled.Tag,
                contentDescription = if (channel.isPrivate) "Private channel" else "Public channel",
                tint = if (isPending) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
            )
        },
        headlineContent = {
            Text(
                text = channel.name,
                color = if (isPending) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
            )
        },
        supportingContent = channel.categoryName?.let { category ->
            {
                Text(
                    text = category,
                    color = if (isPending) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            when {
                isPending -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                }
                isError -> {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Sync error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
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
                    Channel("2", "pending-channel", false, false, false, null, null, null, syncStatus = com.example.shine.domain.model.SyncStatus.PENDING),
                    Channel("3", "error-channel", false, false, false, null, null, null, syncStatus = com.example.shine.domain.model.SyncStatus.ERROR, syncErrorMessage = "Tên kênh đã tồn tại"),
                ),
            ),
            onChannelClick = {},
            onAddChannelClick = {},
        )
    }
}
