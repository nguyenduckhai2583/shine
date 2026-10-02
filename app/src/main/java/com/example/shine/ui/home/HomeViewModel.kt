package com.example.shine.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.domain.model.Channel
import com.example.shine.domain.usecase.GetChannelsUseCase
import com.example.shine.domain.usecase.ObserveSessionUseCase
import com.example.shine.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val displayName: String = "",
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val getChannels: GetChannelsUseCase,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {

    private val channelsState = MutableStateFlow(HomeUiState(isLoading = true))

    val uiState: StateFlow<HomeUiState> = combine(observeSession(), channelsState) { session, state ->
        state.copy(displayName = session?.user?.displayName.orEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(isLoading = true))

    init {
        loadChannels()
    }

    fun loadChannels() {
        channelsState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            getChannels()
                .onSuccess { channels ->
                    channelsState.update { it.copy(channels = channels, isLoading = false) }
                }
                .onFailure { error ->
                    channelsState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch { signOutUseCase() }
    }
}
