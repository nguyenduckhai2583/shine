package com.example.shine.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Channel
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.domain.repository.ChannelRepository
import com.example.shine.ui.common.UiText
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

data class HomeUiState(
    val displayName: String = "",
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository,
) : ViewModel() {

    private val channelsState = MutableStateFlow(HomeUiState(isLoading = true))

    val uiState: StateFlow<HomeUiState> = combine(authRepository.session, channelsState) { session, state ->
        state.copy(displayName = session?.user?.displayName.orEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(isLoading = true))

    init {
        loadChannels()
    }

    fun loadChannels() {
        channelsState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val channels = channelRepository.getChannels()
                channelsState.update { it.copy(channels = channels, isLoading = false) }
            } catch (e: AppException) {
                channelsState.update { it.copy(isLoading = false, errorMessage = e.toUiText()) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
