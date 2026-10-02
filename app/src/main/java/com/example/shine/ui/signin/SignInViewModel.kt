package com.example.shine.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.domain.usecase.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, errorMessage = null) }

    fun onTogglePasswordVisibility() =
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun signIn() {
        val state = _uiState.value
        if (!state.canSubmit) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            // On success the session is persisted; AppViewModel observes it and shows Home.
            signInUseCase(state.email, state.password)
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }
}
