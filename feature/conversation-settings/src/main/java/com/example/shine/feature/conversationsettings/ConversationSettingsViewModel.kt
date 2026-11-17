package com.example.shine.feature.conversationsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.data.repository.ChannelRepository
import com.example.shine.core.data.repository.ConversationSettingRepository
import com.example.shine.core.data.repository.DirectMessageRepository
import com.example.shine.core.model.Channel
import com.example.shine.core.model.Conversation
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConversationSettingsUiState(
    val title: String = "",
    val subtitle: UiText? = null,
    val type: ConversationType = ConversationType.CHANNEL,
    val isNotificationEnabled: Boolean = true,
    val isLoading: Boolean = true,
)

@HiltViewModel(assistedFactory = ConversationSettingsViewModel.Factory::class)
class ConversationSettingsViewModel @AssistedInject constructor(
    @Assisted private val conversationId: String,
    @Assisted conversationType: ConversationType,
    channelRepository: ChannelRepository,
    directMessageRepository: DirectMessageRepository,
    private val conversationSettingRepository: ConversationSettingRepository,
) : ViewModel() {

    val uiState: StateFlow<ConversationSettingsUiState> = conversationSettingsUiState(
        conversationId = conversationId,
        conversationType = conversationType,
        channelRepository = channelRepository,
        directMessageRepository = directMessageRepository,
        conversationSettingRepository = conversationSettingRepository,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConversationSettingsUiState(
            type = conversationType,
            isLoading = true,
        ),
    )

    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            conversationSettingRepository.setNotificationEnabled(conversationId, enabled)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            conversationId: String,
            conversationType: ConversationType,
        ): ConversationSettingsViewModel
    }
}

private fun conversationSettingsUiState(
    conversationId: String,
    conversationType: ConversationType,
    channelRepository: ChannelRepository,
    directMessageRepository: DirectMessageRepository,
    conversationSettingRepository: ConversationSettingRepository,
): Flow<ConversationSettingsUiState> {
    val conversationFlow: Flow<Conversation?> = when (conversationType) {
        ConversationType.CHANNEL -> channelRepository.getChannelFlow(conversationId)
        ConversationType.DM -> directMessageRepository.getDirectMessageFlow(conversationId)
    }

    return combine(
        conversationFlow,
        conversationSettingRepository.getConversationSettingFlow(conversationId),
    ) { conversation, setting ->
        val notifEnabled = setting?.isNotificationEnabled ?: true
        if (conversation == null) {
            ConversationSettingsUiState(
                type = conversationType,
                isNotificationEnabled = notifEnabled,
            )
        } else {
            val subtitle = (conversation as? Channel)?.let {
                if (it.isPrivate) {
                    UiText.Resource(R.string.conversation_settings_private_channel)
                } else {
                    UiText.Resource(R.string.conversation_settings_public_channel)
                }
            }

            ConversationSettingsUiState(
                title = conversation.name,
                subtitle = subtitle,
                type = conversationType,
                isNotificationEnabled = notifEnabled,
                isLoading = false,
            )
        }
    }
}
