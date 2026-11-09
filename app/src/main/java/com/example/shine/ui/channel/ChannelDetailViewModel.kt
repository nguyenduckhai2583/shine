package com.example.shine.ui.channel

import androidx.lifecycle.viewModelScope
import com.example.shine.core.base.BaseViewModel
import com.example.shine.core.shortcut.AppShortcutManager
import com.example.shine.domain.model.Channel
import com.example.shine.domain.repository.ChannelRepository
import com.example.shine.ui.common.UiText
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ChannelDetailUiState {
    data object Loading : ChannelDetailUiState
    data class Success(val channel: Channel) : ChannelDetailUiState
    data class Error(val message: UiText) : ChannelDetailUiState
}

@HiltViewModel(assistedFactory = ChannelDetailViewModel.Factory::class)
class ChannelDetailViewModel @AssistedInject constructor(
    @Assisted private val channelId: String,
    private val channelRepository: ChannelRepository,
    private val shortcutManager: AppShortcutManager,
) : BaseViewModel() {

    private val syncError = MutableStateFlow<UiText?>(null)

    val uiState: StateFlow<ChannelDetailUiState> = combine(
        channelRepository.getChannelFlow(channelId),
        syncError,
    ) { channel, error ->
        when {
            channel != null -> ChannelDetailUiState.Success(channel)
            error != null -> ChannelDetailUiState.Error(error)
            else -> ChannelDetailUiState.Loading
        }
    }.stateIn(
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
        syncError.value = null
        execute(
            task = { channelRepository.getChannel(channelId) },
            onError = { error ->
                syncError.value = error
            },
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(channelId: String): ChannelDetailViewModel
    }
}
