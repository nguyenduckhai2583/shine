package com.example.shine.ui.signin

import com.example.shine.ui.common.UiText

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
