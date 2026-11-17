package com.example.shine.feature.dm

import androidx.lifecycle.viewModelScope
import com.example.shine.core.common.R
import com.example.shine.core.common.base.BaseViewModel
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.data.repository.DirectMessageRepository
import com.example.shine.core.model.DirectMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface DirectMessageDetailUiState {
    data object Loading : DirectMessageDetailUiState
    data class Success(val directMessage: DirectMessage) : DirectMessageDetailUiState
    data class Error(val message: UiText = UiText.Resource(R.string.error_unknown)) : DirectMessageDetailUiState
}

@HiltViewModel(assistedFactory = DirectMessageDetailViewModel.Factory::class)
class DirectMessageDetailViewModel @AssistedInject constructor(
    @Assisted private val dmId: String,
    private val directMessageRepository: DirectMessageRepository,
) : BaseViewModel() {

    val uiState: StateFlow<DirectMessageDetailUiState> = directMessageRepository
        .getDirectMessageFlow(dmId)
        .map { dm ->
            if (dm != null) {
                DirectMessageDetailUiState.Success(dm)
            } else {
                DirectMessageDetailUiState.Error()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DirectMessageDetailUiState.Loading,
        )

    init {
        load()
    }

    fun load() {
        execute(task = { directMessageRepository.getDirectMessage(dmId) })
    }

    @AssistedFactory
    interface Factory {
        fun create(dmId: String): DirectMessageDetailViewModel
    }
}
