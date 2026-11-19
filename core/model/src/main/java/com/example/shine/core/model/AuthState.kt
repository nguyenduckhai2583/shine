package com.example.shine.core.model

sealed interface AuthState {
    data object Initializing : AuthState
    data object Unauthenticated : AuthState
    data object SelectWorkspace : AuthState
    data object Authenticated : AuthState
}
