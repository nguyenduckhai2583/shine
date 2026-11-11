package com.example.shine.feature.home

import androidx.lifecycle.viewModelScope
import com.example.shine.core.common.base.BaseViewModel
import com.example.shine.core.model.Channel
import com.example.shine.core.data.repository.ChannelRepository
import com.example.shine.core.data.repository.WorkspaceRepository
import com.example.shine.core.common.ui.UiText
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
                    currentWorkspaceId = workspaceId
                    loadChannels(isRefresh = false)
                }
        }
    }

    fun refresh() {
        loadChannels(isRefresh = true)
    }

    fun clearUserError() {
        userError.value = null
    }

    fun clearError() {
        clearUserError()
    }

    fun createChannel(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        execute(
            task = { channelRepository.createChannel(trimmed) },
            onError = { userError.value = it },
        )
    }

    fun retryChannel(channelId: String) {
        execute(
            task = { channelRepository.retrySync(channelId) },
            onError = { userError.value = it },
        )
    }

    fun deleteChannel(channelId: String) {
        execute(
            task = { channelRepository.deleteChannel(channelId) },
            onError = { userError.value = it },
        )
    }

    fun deleteFailedChannel(channelId: String) {
        deleteChannel(channelId)
    }

    private fun loadChannels(isRefresh: Boolean) {
        if (currentWorkspaceId == null) return

        if (isRefresh) {
            isRefreshing.value = true
        } else {
            isLoading.value = true
        }

        execute(
            task = { channelRepository.getChannels() },
            onError = { userError.value = it },
            onComplete = {
                isLoading.value = false
                isRefreshing.value = false
            },
        )
    }
}
