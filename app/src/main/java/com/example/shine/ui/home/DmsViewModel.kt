package com.example.shine.ui.home

import androidx.lifecycle.viewModelScope
import com.example.shine.core.base.BaseViewModel
import com.example.shine.domain.model.DirectMessage
import com.example.shine.domain.repository.DirectMessageRepository
import com.example.shine.domain.repository.WorkspaceRepository
import com.example.shine.ui.common.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DmsUiState(
    val dms: List<DirectMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val userError: UiText? = null,
)

@HiltViewModel
class DmsViewModel @Inject constructor(
    private val directMessageRepository: DirectMessageRepository,
    private val workspaceRepository: WorkspaceRepository,
) : BaseViewModel() {

    private val isLoading = MutableStateFlow(true)
    private val isRefreshing = MutableStateFlow(false)
    private val userError = MutableStateFlow<UiText?>(null)

    val uiState: StateFlow<DmsUiState> = combine(
        directMessageRepository.getDirectMessagesFlow(),
        isLoading,
        isRefreshing,
        userError,
    ) { dms, loading, refreshing, error ->
        DmsUiState(
            dms = dms,
            isLoading = loading && dms.isEmpty(),
            isRefreshing = refreshing,
            userError = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DmsUiState(isLoading = true),
    )

    private var currentWorkspaceId: String? = null

    init {
        viewModelScope.launch {
            workspaceRepository.selectedWorkspaceId
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { workspaceId ->
                    if (currentWorkspaceId != null && currentWorkspaceId != workspaceId) {
                        directMessageRepository.clear()
                    }
                    currentWorkspaceId = workspaceId
                    loadInitial()
                }
        }
    }

    private fun loadInitial() {
        isLoading.value = true
        userError.value = null
        execute(
            task = { directMessageRepository.getDirectMessages() },
            onError = { userError.value = it },
            onComplete = { isLoading.value = false },
        )
    }

    fun refresh() {
        isRefreshing.value = true
        userError.value = null
        execute(
            task = { directMessageRepository.getDirectMessages() },
            onError = { userError.value = it },
            onComplete = { isRefreshing.value = false },
        )
    }

    fun clearUserError() {
        userError.value = null
    }
}
