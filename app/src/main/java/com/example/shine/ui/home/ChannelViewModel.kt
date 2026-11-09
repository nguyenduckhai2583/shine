package com.example.shine.ui.home

import androidx.lifecycle.viewModelScope
import com.example.shine.core.base.BaseViewModel
import com.example.shine.domain.model.Channel
import com.example.shine.domain.repository.ChannelRepository
import com.example.shine.domain.repository.WorkspaceRepository
import com.example.shine.ui.common.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelUiState(
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val userError: UiText? = null,
)

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val workspaceRepository: WorkspaceRepository,
) : BaseViewModel() {

    private val isLoading = MutableStateFlow(true)
    private val isRefreshing = MutableStateFlow(false)
    private val userError = MutableStateFlow<UiText?>(null)

    val uiState: StateFlow<ChannelUiState> = combine(
        channelRepository.getChannelsFlow(),
        isLoading,
        isRefreshing,
        userError,
    ) { channels, loading, refreshing, error ->
        ChannelUiState(
            channels = channels,
            isLoading = loading && channels.isEmpty(),
            isRefreshing = refreshing,
            userError = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChannelUiState(isLoading = true),
    )

    private var currentWorkspaceId: String? = null

    init {
        viewModelScope.launch {
            workspaceRepository.selectedWorkspaceId
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { workspaceId ->
                    if (currentWorkspaceId != null && currentWorkspaceId != workspaceId) {
                        channelRepository.clear()
                    }
                    currentWorkspaceId = workspaceId
                    load()
                }
        }
    }

    private fun load() {
        isLoading.value = true
        userError.value = null
        execute(
            task = { channelRepository.getChannels() },
            onError = { userError.value = it },
            onComplete = { isLoading.value = false },
        )
    }

    fun refresh() {
        isRefreshing.value = true
        userError.value = null
        execute(
            task = { channelRepository.getChannels() },
            onError = { userError.value = it },
            onComplete = { isRefreshing.value = false },
        )
    }

    fun createChannel(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        execute(
            task = { channelRepository.createChannel(name = trimmed) },
            onError = { userError.value = it },
        )
    }

    fun deleteChannel(channelId: String) {
        execute(task = { channelRepository.deleteChannel(channelId) })
    }

    fun clearUserError() {
        userError.value = null
    }
}
