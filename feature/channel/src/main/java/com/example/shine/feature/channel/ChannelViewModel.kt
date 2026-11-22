package com.example.shine.feature.channel

import androidx.lifecycle.viewModelScope
import com.example.shine.core.common.base.BaseViewModel
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.data.repository.ChannelRepository
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.model.Channel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelUiState(
    val workspaceId: String? = null,
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val userError: UiText? = null,
)

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val sessionManager: SessionManager,
) : BaseViewModel() {

    private val syncedWorkspaceId = MutableStateFlow<String?>(null)
    private val isLoading = MutableStateFlow(true)
    private val isRefreshing = MutableStateFlow(false)
    private val userError = MutableStateFlow<UiText?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ChannelUiState> = sessionManager.workspaceId
        .filterNotNull()
        .flatMapLatest { workspaceId ->
            combine(
                channelRepository.getChannelsFlow(workspaceId),
                isLoading,
                isRefreshing,
                syncedWorkspaceId,
                userError,
            ) { channels, loading, refreshing, syncedWsId, error ->
                val isWorkspaceLoading = syncedWsId != workspaceId
                ChannelUiState(
                    workspaceId = workspaceId,
                    channels = if (isWorkspaceLoading) emptyList() else channels,
                    isLoading = isWorkspaceLoading || (loading && channels.isEmpty()),
                    isRefreshing = refreshing,
                    userError = error,
                )
            }.onStart {
                emit(
                    ChannelUiState(
                        workspaceId = workspaceId,
                        isLoading = true,
                        channels = emptyList(),
                    )
                )
            }
        }
        .conflate()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ChannelUiState(isLoading = true),
        )

    private var currentWorkspaceId: String? = null

    init {
        viewModelScope.launch {
            sessionManager.workspaceId
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
        val workspaceId = currentWorkspaceId ?: return
        execute(
            task = { channelRepository.createChannel(trimmed, workspaceId) },
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
        val workspaceId = currentWorkspaceId ?: return

        if (isRefresh) {
            isRefreshing.value = true
        } else {
            isLoading.value = true
        }

        execute(
            task = { channelRepository.getChannels(workspaceId) },
            onError = { userError.value = it },
            onComplete = {
                syncedWorkspaceId.value = workspaceId
                isLoading.value = false
                isRefreshing.value = false
            },
        )
    }
}
