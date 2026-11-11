package com.example.shine.feature.home

import androidx.lifecycle.viewModelScope
import com.example.shine.core.common.base.BaseViewModel
import com.example.shine.core.shortcut.AppShortcutManager
import com.example.shine.core.model.Workspace
import com.example.shine.core.data.repository.AuthRepository
import com.example.shine.core.data.repository.UserRepository
import com.example.shine.core.data.repository.WorkspaceRepository
import com.example.shine.core.domain.SignOutUseCase
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
    val avatarUrl: String? = null,
    val workspaces: List<Workspace> = emptyList(),
    val selectedWorkspaceId: String? = null,
    val isSwitchingWorkspace: Boolean = false,
) {
    val currentWorkspaceName: String
        get() = workspaces.firstOrNull { it.id == selectedWorkspaceId }?.name.orEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val userRepository: UserRepository,
    private val shortcutManager: AppShortcutManager,
    private val signOutUseCase: SignOutUseCase,
) : BaseViewModel() {

    private val isSwitchingWorkspace = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        authRepository.session,
        workspaceRepository.workspaces,
        isSwitchingWorkspace,
    ) { session, workspaces, switching ->
        HomeUiState(
            displayName = session?.user?.displayName.orEmpty(),
            userEmail = session?.user?.email.orEmpty(),
            avatarUrl = session?.user?.avatarUrl,
            workspaces = workspaces,
            selectedWorkspaceId = session?.workspaceId,
            isSwitchingWorkspace = switching,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    init {
        loadWorkspaces()
    }

    fun loadWorkspaces() {
        viewModelScope.launch {
            workspaceRepository.fetchWorkspaces()
        }
    }

    fun selectWorkspace(workspaceId: String) {
        if (workspaceId == uiState.value.selectedWorkspaceId) return
        viewModelScope.launch {
            isSwitchingWorkspace.value = true
            try {
                shortcutManager.removeRecentChannelShortcut()
                userRepository.getChatUsers(workspaceId)
                workspaceRepository.selectWorkspace(workspaceId)
            } finally {
                isSwitchingWorkspace.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            shortcutManager.removeAllDynamicShortcuts()
            signOutUseCase()
        }
    }
}
