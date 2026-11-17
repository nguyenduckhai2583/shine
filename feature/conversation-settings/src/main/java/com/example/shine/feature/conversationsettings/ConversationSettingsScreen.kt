package com.example.shine.feature.conversationsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.designsystem.component.ShineTopAppBar
import com.example.shine.core.designsystem.theme.ShineTheme

@Composable
fun ConversationSettingsRoute(
    conversationId: String,
    conversationType: ConversationType,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    viewModel: ConversationSettingsViewModel = hiltViewModel<ConversationSettingsViewModel, ConversationSettingsViewModel.Factory>(
        creationCallback = { factory -> factory.create(conversationId, conversationType) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ConversationSettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onToggleNotification = viewModel::setNotificationEnabled,
        modifier = modifier,
    )
}

@Composable
fun ConversationSettingsScreen(
    uiState: ConversationSettingsUiState,
    onToggleNotification: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ShineTopAppBar(
                title = stringResource(
                    when (uiState.type) {
                        ConversationType.CHANNEL -> R.string.conversation_settings_title_channel
                        ConversationType.DM -> R.string.conversation_settings_title_dm
                    },
                ),
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
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                ConversationSettingsContent(
                    uiState = uiState,
                    onToggleNotification = onToggleNotification,
                )
            }
        }
    }
}

@Composable
private fun ConversationSettingsContent(
    uiState: ConversationSettingsUiState,
    onToggleNotification: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.type == ConversationType.CHANNEL) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Text(
                        text = uiState.title.take(1).uppercase().ifEmpty { "D" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (uiState.type == ConversationType.CHANNEL) "#${uiState.title}" else uiState.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            uiState.subtitle?.let { subtitle ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle.asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider()

        // Notification Settings Section
        Text(
            text = stringResource(R.string.conversation_settings_section_notifications),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        )

        ListItem(
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                )
            },
            headlineContent = {
                Text(text = stringResource(R.string.conversation_settings_enable_notifications))
            },
            supportingContent = {
                Text(text = stringResource(R.string.conversation_settings_enable_notifications_description))
            },
            trailingContent = {
                Switch(
                    checked = uiState.isNotificationEnabled,
                    onCheckedChange = onToggleNotification,
                )
            },
        )

        HorizontalDivider()
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationSettingsChannelPreview() {
    ShineTheme(dynamicColor = false) {
        ConversationSettingsScreen(
            uiState = ConversationSettingsUiState(
                title = "general",
                subtitle = UiText.Resource(R.string.conversation_settings_public_channel),
                type = ConversationType.CHANNEL,
                isNotificationEnabled = true,
                isLoading = false,
            ),
            onBack = {},
            onToggleNotification = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationSettingsDmPreview() {
    ShineTheme(dynamicColor = false) {
        ConversationSettingsScreen(
            uiState = ConversationSettingsUiState(
                title = "Alice Smith",
                subtitle = null,
                type = ConversationType.DM,
                isNotificationEnabled = false,
                isLoading = false,
            ),
            onBack = {},
            onToggleNotification = {},
        )
    }
}
