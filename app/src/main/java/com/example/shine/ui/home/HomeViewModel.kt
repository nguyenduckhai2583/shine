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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val displayName: String = "",
    val userEmail: String = "",
    val channels: List<Channel> = emptyList(),
    val workspaces: List<Workspace> = emptyList(),
    val selectedWorkspaceId: String? = null,
    val selectedTab: HomeTab = HomeTab.Channels,
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

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeData()
        loadWorkspaces()
        loadChannels()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                authRepository.session,
                channelRepository.getChannelsFlow(),
                workspaceRepository.workspaces,
            ) { session, channels, workspaces ->
                _uiState.update { current ->
                    current.copy(
                        displayName = session?.user?.displayName.orEmpty(),
                        userEmail = session?.user?.email.orEmpty(),
                        channels = channels,
                        workspaces = workspaces,
                        selectedWorkspaceId = session?.workspaceId,
                        isLoading = current.isLoading && channels.isEmpty(),
                    )
                }
            }.collectLatest {}
        }
    }

    fun selectTab(tab: HomeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun loadWorkspaces() {
        viewModelScope.launch {
            try {
                workspaceRepository.fetchWorkspaces()
            } catch (_: Exception) {}
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

    fun selectWorkspace(workspaceId: String) {
        if (workspaceId == _uiState.value.selectedWorkspaceId) return
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
