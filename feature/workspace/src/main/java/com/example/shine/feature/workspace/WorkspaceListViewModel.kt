package com.example.shine.feature.workspace

import androidx.lifecycle.viewModelScope
import com.example.shine.core.common.base.BaseViewModel
import com.example.shine.core.model.Workspace
import com.example.shine.core.data.repository.AuthRepository
import com.example.shine.core.data.repository.WorkspaceRepository
import com.example.shine.core.common.ui.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkspaceListUiState(
    val workspaces: List<Workspace> = emptyList(),
    val selectedWorkspaceId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
) {
    val canSubmit: Boolean
        get() = selectedWorkspaceId != null && !isLoading
}

@HiltViewModel
class WorkspaceListViewModel @Inject constructor(
    private val workspaceRepository: WorkspaceRepository,
    private val authRepository: AuthRepository,
) : BaseViewModel() {

    private val selectedWorkspaceId = MutableStateFlow<String?>(null)
    private val isSyncing = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<UiText?>(null)

    val uiState: StateFlow<WorkspaceListUiState> = combine(
        workspaceRepository.workspaces,
        selectedWorkspaceId,
        isSyncing,
        errorMessage,
    ) { workspaces, selectedId, syncing, error ->
        val effectiveSelectedId = selectedId ?: workspaces.firstOrNull()?.id
        WorkspaceListUiState(
            workspaces = workspaces,
            selectedWorkspaceId = effectiveSelectedId,
            isLoading = syncing && workspaces.isEmpty(),
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkspaceListUiState(isLoading = true),
    )

    init {
        loadWorkspaces()
    }

    fun loadWorkspaces() {
        isSyncing.value = true
        errorMessage.value = null
        execute(
            task = { workspaceRepository.fetchWorkspaces() },
            onSuccess = {
                isSyncing.value = false
            },
            onError = { error ->
                isSyncing.value = false
                errorMessage.value = error
            },
        )
    }

    fun onSelectWorkspace(workspaceId: String) {
        selectedWorkspaceId.value = workspaceId
    }

    fun confirmSelection() {
        val id = uiState.value.selectedWorkspaceId ?: return
        viewModelScope.launch {
            workspaceRepository.selectWorkspace(id)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
