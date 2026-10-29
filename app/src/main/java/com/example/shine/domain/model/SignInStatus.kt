package com.example.shine.domain.model

sealed interface SignInStatus {
    data class Authenticated(val session: Session) : SignInStatus
    data class TwoFactorRequired(val tmpToken: String) : SignInStatus
}

