package com.example.shine.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shine.domain.model.DirectMessage
import com.example.shine.ui.common.TimeFormatter
import com.example.shine.ui.home.DmsUiState
import com.example.shine.ui.home.DmsViewModel
import com.example.shine.ui.theme.ShineTheme

@Composable
fun DmsRoute(
    modifier: Modifier = Modifier,
    onDmClick: (DirectMessage) -> Unit = {},
    viewModel: DmsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.userError) {
        val error = uiState.userError
        if (error != null) {
            snackbarHostState.showSnackbar(error.asString(context))
            viewModel.clearUserError()
        }
    }

    DmsContent(
        uiState = uiState,
        onDmClick = onDmClick,
        onRefresh = viewModel::refresh,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun DmsContent(
    uiState: DmsUiState,
    onDmClick: (DirectMessage) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.dms.isEmpty() -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No direct messages yet")
                    }
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.dms, key = { it.id }) { dm ->
                            DirectMessageItem(dm = dm, onClick = { onDmClick(dm) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun DirectMessageItem(
    dm: DirectMessage,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = {
            Text(
                text = dm.name,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        trailingContent = {
            val formattedTime = TimeFormatter.formatTimeAgo(dm.time)
            if (formattedTime.isNotEmpty()) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

fun formatDmTime(seconds: Long?): String = TimeFormatter.formatTimeAgo(seconds)

@Preview(showBackground = true)
@Composable
private fun DmsContentPreview() {
    ShineTheme(dynamicColor = false) {
        DmsContent(
            uiState = DmsUiState(
                dms = listOf(
                    DirectMessage("1", "Alice Smith", System.currentTimeMillis() / 1000 - 300),
                    DirectMessage("2", "Bob Johnson", System.currentTimeMillis() / 1000 - 7200),
                ),
            ),
            onDmClick = {},
            onRefresh = {},
        )
    }
}
