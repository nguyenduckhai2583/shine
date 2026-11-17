package com.example.shine.feature.home.dm

import androidx.lifecycle.viewModelScope
import com.example.shine.core.common.base.BaseViewModel
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.data.repository.DirectMessageRepository
import com.example.shine.core.model.DirectMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface DirectMessageDetailUiState {
    data object Loading : DirectMessageDetailUiState
    data class Success(val directMessage: DirectMessage) : DirectMessageDetailUiState
    data class Error(val message: UiText) : DirectMessageDetailUiState
}

@HiltViewModel(assistedFactory = DirectMessageDetailViewModel.Factory::class)
class DirectMessageDetailViewModel @AssistedInject constructor(
    @Assisted private val dmId: String,
    private val directMessageRepository: DirectMessageRepository,
) : BaseViewModel() {

    private val syncError = MutableStateFlow<UiText?>(null)

    val uiState: StateFlow<DirectMessageDetailUiState> = combine(
        directMessageRepository.getDirectMessageFlow(dmId),
        syncError,
    ) { dm, error ->
        when {
            dm != null -> DirectMessageDetailUiState.Success(dm)
            error != null -> DirectMessageDetailUiState.Error(error)
            else -> DirectMessageDetailUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DirectMessageDetailUiState.Loading,
    )

    init {
        load()
    }

    fun load() {
        syncError.value = null
        execute(
            task = { directMessageRepository.getDirectMessage(dmId) },
            onError = { error ->
                syncError.value = error
            },
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(dmId: String): DirectMessageDetailViewModel
    }
}
