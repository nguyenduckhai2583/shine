package com.example.shine.ui.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.domain.model.AppException
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.domain.repository.WorkspaceRepository
import com.example.shine.ui.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkspaceListViewModel @Inject constructor(
    private val workspaceRepository: WorkspaceRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val selectedWorkspaceId = MutableStateFlow<String?>(null)
    private val isLoading = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<com.example.shine.ui.common.UiText?>(null)

    val uiState: StateFlow<WorkspaceListUiState> = combine(
        workspaceRepository.workspaces,
        selectedWorkspaceId,
        isLoading,
        errorMessage,
    ) { workspaces, selectedId, loading, error ->
        WorkspaceListUiState(
            workspaces = workspaces,
            selectedWorkspaceId = selectedId ?: workspaces.firstOrNull()?.id,
            isLoading = loading,
            errorMessage = error,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        WorkspaceListUiState(isLoading = true),
    )

    init {
        loadWorkspaces()
    }

    fun loadWorkspaces() {
        isLoading.value = true
        errorMessage.value = null
        viewModelScope.launch {
            try {
                workspaceRepository.fetchWorkspaces()
            } catch (e: AppException) {
                errorMessage.value = e.toUiText()
            } catch (_: Exception) {
                errorMessage.value = com.example.shine.ui.common.UiText.Raw("Failed to load workspaces")
            } finally {
                isLoading.value = false
            }
        }
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
