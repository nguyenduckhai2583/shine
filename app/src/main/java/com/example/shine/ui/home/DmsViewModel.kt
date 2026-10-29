package com.example.shine.ui.home

import androidx.lifecycle.viewModelScope
import com.example.shine.core.base.BaseViewModel
import com.example.shine.domain.model.DirectMessage
import com.example.shine.domain.repository.DirectMessageRepository
import com.example.shine.domain.repository.WorkspaceRepository
import com.example.shine.ui.common.UiText
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
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(DmsUiState(isLoading = true))
    val uiState: StateFlow<DmsUiState> = _uiState.asStateFlow()

    private var currentWorkspaceId: String? = null

    init {
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

        viewModelScope.launch {
            workspaceRepository.selectedWorkspaceId
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { workspaceId ->
                    if (currentWorkspaceId != null && currentWorkspaceId != workspaceId) {
                        directMessageRepository.clear()
                    }
                    currentWorkspaceId = workspaceId
                    loadDms()
                }
        }
    }

    fun loadDms() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        execute(
            task = { directMessageRepository.getDirectMessages() },
            onSuccess = {
                _uiState.update { it.copy(isLoading = false) }
            },
            onError = { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error) }
            }
        )
    }
}
