package com.example.shine.ui.home

import androidx.lifecycle.viewModelScope
import com.example.shine.R
import com.example.shine.core.base.BaseViewModel
import com.example.shine.domain.model.Channel
import com.example.shine.domain.repository.ChannelRepository
import com.example.shine.domain.repository.WorkspaceRepository
import com.example.shine.ui.common.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelUiState(
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val snackbarMessage: UiText? = null,
)

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val workspaceRepository: WorkspaceRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ChannelUiState(isLoading = true))
    val uiState: StateFlow<ChannelUiState> = _uiState.asStateFlow()

    private var currentWorkspaceId: String? = null

    init {
        viewModelScope.launch {
            channelRepository.getChannelsFlow().collectLatest { channels ->
                _uiState.update { current ->
                    current.copy(
                        channels = channels,
                        isLoading = current.isLoading && channels.isEmpty(),
                    )
                }
            }
        }

        viewModelScope.launch {
            workspaceRepository.selectedWorkspaceId
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { workspaceId ->
                    if (currentWorkspaceId != null && currentWorkspaceId != workspaceId) {
                        channelRepository.clear()
                    }
                    currentWorkspaceId = workspaceId
                    loadChannels()
                }
        }
    }

    fun loadChannels() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        execute(
            task = { channelRepository.getChannels() },
            onSuccess = {
                _uiState.update { it.copy(isLoading = false) }
            },
            onError = { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error) }
            }
        )
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun createChannel(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        execute(
            task = { channelRepository.createChannel(name = trimmed) },
            onSuccess = {
                _uiState.update {
                    it.copy(snackbarMessage = UiText(R.string.channel_created_success, "#$trimmed"))
                }
            },
            onError = { error ->
                _uiState.update { it.copy(snackbarMessage = error) }
            }
        )
    }
}
