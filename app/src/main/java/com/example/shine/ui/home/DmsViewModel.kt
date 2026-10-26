package com.example.shine.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.DirectMessage
import com.example.shine.domain.repository.DirectMessageRepository
import com.example.shine.domain.repository.WorkspaceRepository
import com.example.shine.ui.common.UiText
import com.example.shine.ui.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DmsUiState(
    val dms: List<DirectMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
)

@HiltViewModel
class DmsViewModel @Inject constructor(
    private val directMessageRepository: DirectMessageRepository,
    private val workspaceRepository: WorkspaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DmsUiState(isLoading = true))
    val uiState: StateFlow<DmsUiState> = _uiState.asStateFlow()

    private var currentWorkspaceId: String? = null

    init {
        observeDms()
        observeWorkspace()
        loadDms()
    }

    private fun observeDms() {
        viewModelScope.launch {
            directMessageRepository.getDirectMessagesFlow().collectLatest { dms ->
                _uiState.update { current ->
                    current.copy(
                        dms = dms,
                        isLoading = current.isLoading && dms.isEmpty(),
                    )
                }
            }
        }
    }

    private fun observeWorkspace() {
        viewModelScope.launch {
            workspaceRepository.selectedWorkspaceId
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { workspaceId ->
                    if (currentWorkspaceId != null && currentWorkspaceId != workspaceId) {
                        directMessageRepository.clear()
                        loadDms()
                    }
                    currentWorkspaceId = workspaceId
                }
        }
    }

    fun loadDms() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                directMessageRepository.getDirectMessages()
            } catch (e: AppException) {
                _uiState.update { it.copy(errorMessage = e.toUiText()) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
