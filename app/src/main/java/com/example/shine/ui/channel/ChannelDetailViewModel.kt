package com.example.shine.ui.channel

import com.example.shine.core.base.BaseViewModel
import com.example.shine.domain.model.Channel
import com.example.shine.domain.repository.ChannelRepository
import com.example.shine.ui.common.UiText
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ChannelDetailUiState(
    val channel: Channel? = null,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
)

@HiltViewModel(assistedFactory = ChannelDetailViewModel.Factory::class)
class ChannelDetailViewModel @AssistedInject constructor(
    @Assisted private val channelId: String,
    private val channelRepository: ChannelRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ChannelDetailUiState(isLoading = true))
    val uiState: StateFlow<ChannelDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        execute(
            task = { channelRepository.getChannel(channelId) },
            onSuccess = { channel ->
                _uiState.update { it.copy(channel = channel, isLoading = false) }
            },
            onError = { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error) }
            }
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(channelId: String): ChannelDetailViewModel
    }
}
