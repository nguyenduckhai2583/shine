package com.example.shine.feature.channel

import androidx.lifecycle.viewModelScope
import com.example.shine.core.common.R
import com.example.shine.core.common.base.BaseViewModel
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.data.repository.ChannelRepository
import com.example.shine.core.model.Channel
import com.example.shine.core.shortcut.AppShortcutManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ChannelDetailUiState {
    data object Loading : ChannelDetailUiState
    data class Success(val channel: Channel) : ChannelDetailUiState
    data class Error(val message: UiText = UiText.Resource(R.string.error_unknown)) : ChannelDetailUiState
}

@HiltViewModel(assistedFactory = ChannelDetailViewModel.Factory::class)
class ChannelDetailViewModel @AssistedInject constructor(
    @Assisted private val channelId: String,
    private val channelRepository: ChannelRepository,
    private val shortcutManager: AppShortcutManager,
) : BaseViewModel() {

    val uiState: StateFlow<ChannelDetailUiState> = channelRepository
        .getChannelFlow(channelId)
        .map { channel ->
            if (channel != null) {
                ChannelDetailUiState.Success(channel)
            } else {
                ChannelDetailUiState.Error()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ChannelDetailUiState.Loading,
        )

    init {
        viewModelScope.launch {
            channelRepository.getChannelFlow(channelId)
                .filterNotNull()
                .first()
                .let { (id, name) -> shortcutManager.updateRecentChannelShortcut(id, name) }
        }
        load()
    }

    fun load() {
        execute(task = { channelRepository.getChannel(channelId) })
    }

    @AssistedFactory
    interface Factory {
        fun create(channelId: String): ChannelDetailViewModel
    }
}
