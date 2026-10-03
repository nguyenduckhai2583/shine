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

    private val isLoading = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<UiText?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        authRepository.session,
        channelRepository.getChannelsFlow(),
        isLoading,
        errorMessage,
    ) { session, channels, loading, error ->
        HomeUiState(
            displayName = session?.user?.displayName.orEmpty(),
            channels = channels,
            isLoading = loading && channels.isEmpty(),
            errorMessage = error,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState(isLoading = true),
    )

    init {
        loadChannels()
    }

    fun loadChannels() {
        isLoading.value = true
        errorMessage.value = null
        viewModelScope.launch {
            try {
                channelRepository.getChannels()
            } catch (e: AppException) {
                errorMessage.value = e.toUiText()
            } finally {
                isLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
