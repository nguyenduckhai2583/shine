package com.example.shine.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Channel
import com.example.shine.domain.model.Workspace
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.domain.repository.ChannelRepository
import com.example.shine.domain.repository.WorkspaceRepository
import com.example.shine.ui.common.UiText
import com.example.shine.ui.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val displayName: String = "",
    val userEmail: String = "",
    val channels: List<Channel> = emptyList(),
    val workspaces: List<Workspace> = emptyList(),
    val selectedWorkspaceId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
) {
    val currentWorkspaceName: String
        get() = workspaces.firstOrNull { it.id == selectedWorkspaceId }?.name.orEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository,
    private val workspaceRepository: WorkspaceRepository,
) : ViewModel() {

    private val isLoading = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<UiText?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        authRepository.session,
        channelRepository.getChannelsFlow(),
        workspaceRepository.workspaces,
        isLoading,
        errorMessage,
    ) { session, channels, workspaces, loading, error ->
        HomeUiState(
            displayName = session?.user?.displayName.orEmpty(),
            userEmail = session?.user?.email.orEmpty(),
            channels = channels,
            workspaces = workspaces,
            selectedWorkspaceId = session?.workspaceId,
            isLoading = loading && channels.isEmpty(),
            errorMessage = error,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState(isLoading = true),
    )

    init {
        loadWorkspaces()
        loadChannels()
    }

    fun loadWorkspaces() {
        viewModelScope.launch {
            try {
                workspaceRepository.fetchWorkspaces()
            } catch (_: Exception) {}
        }
    }

    fun loadChannels() {
        isLoading.value = true
        errorMessage.value = null
        viewModelScope.launch {
            try {
                channelRepository.getChannels()
            } catch (e: AppException) {
                errorMessage.value = e.toUiText()
            } finally {
                isLoading.value = false
            }
        }
    }

    fun selectWorkspace(workspaceId: String) {
        if (workspaceId == uiState.value.selectedWorkspaceId) return
        viewModelScope.launch {
            channelRepository.clear()
            workspaceRepository.selectWorkspace(workspaceId)
            loadChannels()
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
