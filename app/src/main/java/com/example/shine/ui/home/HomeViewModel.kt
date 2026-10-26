package com.example.shine.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.domain.model.Workspace
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.domain.repository.WorkspaceRepository
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
    val avatarUrl: String? = null,
    val workspaces: List<Workspace> = emptyList(),
    val selectedWorkspaceId: String? = null,
    val selectedTab: HomeTab = HomeTab.Channels,
) {
    val currentWorkspaceName: String
        get() = workspaces.firstOrNull { it.id == selectedWorkspaceId }?.name.orEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val workspaceRepository: WorkspaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeData()
        loadWorkspaces()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                authRepository.session,
                workspaceRepository.workspaces,
            ) { session, workspaces ->
                _uiState.update { current ->
                    current.copy(
                        displayName = session?.user?.displayName.orEmpty(),
                        userEmail = session?.user?.email.orEmpty(),
                        avatarUrl = session?.user?.avatarUrl,
                        workspaces = workspaces,
                        selectedWorkspaceId = session?.workspaceId,
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

    fun selectWorkspace(workspaceId: String) {
        if (workspaceId == _uiState.value.selectedWorkspaceId) return
        viewModelScope.launch {
            workspaceRepository.selectWorkspace(workspaceId)
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}

