package com.example.shine.ui.signin

import com.example.shine.core.base.BaseViewModel
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.ui.common.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
)

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

val SignInUiState.isEmailValid: Boolean
    get() = EMAIL_REGEX.matches(email.trim())

val SignInUiState.canSubmit: Boolean
    get() = isEmailValid && password.isNotBlank() && !isLoading

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : BaseViewModel() {

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
        execute(
            task = { authRepository.signIn(email = state.email.trim(), password = state.password) },
            onSuccess = {
                _uiState.update { it.copy(isLoading = false) }
            },
            onError = { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error) }
            },
        )
    }
}
