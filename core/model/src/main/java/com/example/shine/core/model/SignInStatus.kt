package com.example.shine.core.model

sealed interface SignInStatus {
    data class Authenticated(val session: Session) : SignInStatus
    data class TwoFactorRequired(val tmpToken: String) : SignInStatus
}
