package com.example.shine.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeUserSession()
    }

    private fun observeUserSession() {
        viewModelScope.launch {
            authRepository.session.collectLatest { session ->
                val user = session?.user
                _uiState.update { current ->
                    current.copy(
                        name = user?.displayName.orEmpty(),
                        avatarUrl = user?.avatarUrl,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onAvatarSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                selectedAvatarUri = uri,
                isUploading = true,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            try {
                userRepository.updateAvatar(uri)
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        selectedAvatarUri = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        selectedAvatarUri = null,
                        errorMessage = e.message ?: "Failed to update avatar",
                    )
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
