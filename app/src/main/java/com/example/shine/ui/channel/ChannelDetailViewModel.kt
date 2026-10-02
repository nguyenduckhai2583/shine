package com.example.shine.ui.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Channel
import com.example.shine.domain.repository.ChannelRepository
import com.example.shine.ui.common.UiText
import com.example.shine.ui.common.toUiText
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChannelDetailUiState(
    val channel: Channel? = null,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
)

@HiltViewModel(assistedFactory = ChannelDetailViewModel.Factory::class)
class ChannelDetailViewModel @AssistedInject constructor(
    @Assisted private val channelId: String,
    private val channelRepository: ChannelRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelDetailUiState(isLoading = true))
    val uiState: StateFlow<ChannelDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val channel = channelRepository.getChannel(channelId)
                _uiState.update { it.copy(channel = channel, isLoading = false) }
            } catch (e: AppException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.toUiText()) }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(channelId: String): ChannelDetailViewModel
    }
}
