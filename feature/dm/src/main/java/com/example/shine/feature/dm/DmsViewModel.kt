package com.example.shine.feature.dm

import androidx.lifecycle.viewModelScope
import com.example.shine.core.common.base.BaseViewModel
import com.example.shine.core.model.DirectMessage
import com.example.shine.core.data.repository.DirectMessageRepository
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.common.ui.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DmsUiState(
    val workspaceId: String? = null,
    val dms: List<DirectMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val userError: UiText? = null,
)

@HiltViewModel
class DmsViewModel @Inject constructor(
    private val directMessageRepository: DirectMessageRepository,
    private val sessionManager: SessionManager,
) : BaseViewModel() {

    private val syncedWorkspaceId = MutableStateFlow<String?>(null)
    private val isLoading = MutableStateFlow(true)
    private val isRefreshing = MutableStateFlow(false)
    private val userError = MutableStateFlow<UiText?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DmsUiState> = sessionManager.workspaceId
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { workspaceId ->
            combine(
                directMessageRepository.getDirectMessagesFlow(),
                isLoading,
                isRefreshing,
                syncedWorkspaceId,
                userError,
            ) { dms, loading, refreshing, syncedWsId, error ->
                val isWorkspaceLoading = syncedWsId != workspaceId
                DmsUiState(
                    workspaceId = workspaceId,
                    dms = if (isWorkspaceLoading) emptyList() else dms,
                    isLoading = isWorkspaceLoading || (loading && dms.isEmpty()),
                    isRefreshing = refreshing,
                    userError = error,
                )
            }.onStart {
                emit(
                    DmsUiState(
                        workspaceId = workspaceId,
                        isLoading = true,
                        dms = emptyList(),
                    )
                )
            }
        }
        .conflate()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DmsUiState(isLoading = true),
        )

    private var currentWorkspaceId: String? = null

    init {
        viewModelScope.launch {
            sessionManager.workspaceId
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { workspaceId ->
                    currentWorkspaceId = workspaceId
                    loadInitial()
                }
        }
    }

    private fun loadInitial() {
        val workspaceId = currentWorkspaceId
        isLoading.value = true
        userError.value = null
        execute(
            task = { directMessageRepository.getDirectMessages() },
            onError = { userError.value = it },
            onComplete = {
                syncedWorkspaceId.value = workspaceId
                isLoading.value = false
            },
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
