package com.example.shine.ui.workspace

import androidx.lifecycle.viewModelScope
import com.example.shine.core.base.BaseViewModel
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.domain.repository.WorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkspaceListViewModel @Inject constructor(
    private val workspaceRepository: WorkspaceRepository,
    private val authRepository: AuthRepository,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(WorkspaceListUiState(isLoading = true))
    val uiState: StateFlow<WorkspaceListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            workspaceRepository.workspaces.collectLatest { workspaces ->
                _uiState.update { current ->
                    current.copy(
                        workspaces = workspaces,
                        selectedWorkspaceId = current.selectedWorkspaceId ?: workspaces.firstOrNull()?.id,
                        isLoading = current.isLoading && workspaces.isEmpty(),
                    )
                }
            }
        }
        loadWorkspaces()
    }

    fun loadWorkspaces() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        execute(
            task = { workspaceRepository.fetchWorkspaces() },
            onSuccess = {
                _uiState.update { it.copy(isLoading = false) }
            },
            onError = { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error) }
            }
        )
    }

    fun onSelectWorkspace(workspaceId: String) {
        _uiState.update { it.copy(selectedWorkspaceId = workspaceId) }
    }

    fun confirmSelection() {
        val id = _uiState.value.selectedWorkspaceId ?: return
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
