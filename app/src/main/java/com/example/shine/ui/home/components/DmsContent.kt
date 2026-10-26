package com.example.shine.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
    DmsContent(
        uiState = uiState,
        onDmClick = onDmClick,
        modifier = modifier,
    )
}

@Composable
fun DmsContent(
    uiState: DmsUiState,
    onDmClick: (DirectMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading && uiState.dms.isEmpty() -> CircularProgressIndicator()
            uiState.errorMessage != null && uiState.dms.isEmpty() -> Text(
                text = uiState.errorMessage.asString(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp),
            )
            uiState.dms.isEmpty() -> Text("No direct messages yet")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.dms, key = { it.id }) { dm ->
                    DirectMessageItem(dm = dm, onClick = { onDmClick(dm) })
                    HorizontalDivider()
                }
            }
        }
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
        )
    }
}
