package com.example.shine.feature.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shine.feature.home.R
import com.example.shine.core.model.Channel
import com.example.shine.core.model.SyncStatus
import com.example.shine.feature.home.ChannelUiState
import com.example.shine.feature.home.ChannelViewModel
import com.example.shine.core.designsystem.theme.ShineTheme

@Composable
fun ChannelsRoute(
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChannelViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userError) {
        uiState.userError?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create channel",
                )
            }
        },
    ) { innerPadding ->
        ChannelsContent(
            uiState = uiState,
            onChannelClick = onChannelClick,
            onRefresh = viewModel::refresh,
            onDeleteChannel = viewModel::deleteFailedChannel,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.padding(innerPadding),
        )

        if (showCreateBottomSheet) {
            CreateChannelDialog(
                onDismiss = { showCreateBottomSheet = false },
                onCreateChannel = { name ->
                    viewModel.createChannel(name)
                    showCreateBottomSheet = false
                },
            )
        }
    }
}

@Composable
fun ChannelsContent(
    uiState: ChannelUiState,
    onChannelClick: (Channel) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteChannel: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var errorChannelToHandle by remember { mutableStateOf<Channel?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            uiState.channels.isEmpty() -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
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
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp),
                    ) {
                        items(uiState.channels, key = { it.id }) { channel ->
                            ChannelItem(
                                channel = channel,
                                onClick = {
                                    when (channel.syncStatus) {
                                        SyncStatus.SYNCED -> onChannelClick(channel)
                                        SyncStatus.ERROR -> errorChannelToHandle = channel
                                        SyncStatus.PENDING -> { /* Disabled */ }
                                    }
                                },
                                modifier = Modifier.animateItem(),
                            )
                            HorizontalDivider(modifier = Modifier.animateItem())
                        }
                    }
                }
            }
        }

        if (!uiState.isLoading) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp),
            )
        }

        errorChannelToHandle?.let { channel ->
            AlertDialog(
                onDismissRequest = { errorChannelToHandle = null },
                title = { Text(stringResource(R.string.channel_error_dialog_title)) },
                text = {
                    Text(
                        channel.syncErrorMessage
                            ?: stringResource(
                                R.string.channel_error_dialog_default_message,
                                channel.name,
                            )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteChannel(channel.id)
                            errorChannelToHandle = null
                        }
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { errorChannelToHandle = null }
                    ) {
                        Text(stringResource(android.R.string.cancel))
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
    val isPending = channel.syncStatus == SyncStatus.PENDING
    val isError = channel.syncStatus == SyncStatus.ERROR
    val isEnabled = channel.syncStatus == SyncStatus.SYNCED || isError

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
                style = MaterialTheme.typography.titleMedium,
                color = if (isPending) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
            )
        },
        supportingContent = channel.categoryName?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AnimatedVisibility(
                    visible = isPending,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = "Syncing",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                AnimatedVisibility(
                    visible = isError,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = "Sync failed",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun ChannelsContentPreview() {
    ShineTheme(dynamicColor = false) {
        ChannelsContent(
            uiState = ChannelUiState(
                channels = listOf(
                    Channel("1", "general",
                        isPrivate = false,
                        isEncrypted = false,
                        isDefault = true,
                        categoryName = null,
                        createdAt = null,
                        lastActivityAt = null
                    ),
                    Channel("2", "pending-channel",
                        isPrivate = false,
                        isEncrypted = false,
                        isDefault = false,
                        categoryName = null,
                        createdAt = null,
                        lastActivityAt = null,
                        syncStatus = SyncStatus.PENDING
                    ),
                    Channel("3", "error-channel",
                        isPrivate = false,
                        isEncrypted = false,
                        isDefault = false,
                        categoryName = null,
                        createdAt = null,
                        lastActivityAt = null,
                        syncStatus = SyncStatus.ERROR,
                        syncErrorMessage = "Tên kênh đã tồn tại"
                    ),
                ),
            ),
            onChannelClick = {},
            onRefresh = {},
        )
    }
}
