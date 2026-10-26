package com.example.shine.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Channel
import com.example.shine.domain.repository.ChannelRepository
import com.example.shine.domain.repository.WorkspaceRepository
import com.example.shine.ui.common.UiText
import com.example.shine.ui.common.toUiText
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
)

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val workspaceRepository: WorkspaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelUiState(isLoading = true))
    val uiState: StateFlow<ChannelUiState> = _uiState.asStateFlow()

    private var currentWorkspaceId: String? = null

    init {
        observeChannels()
        observeWorkspace()
        loadChannels()
    }

    private fun observeChannels() {
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
    }

    private fun observeWorkspace() {
        viewModelScope.launch {
            workspaceRepository.selectedWorkspaceId
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { workspaceId ->
                    if (currentWorkspaceId != null && currentWorkspaceId != workspaceId) {
                        channelRepository.clear()
                        loadChannels()
                    }
                    currentWorkspaceId = workspaceId
                }
        }
    }

    fun loadChannels() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                channelRepository.getChannels()
            } catch (e: AppException) {
                _uiState.update { it.copy(errorMessage = e.toUiText()) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
