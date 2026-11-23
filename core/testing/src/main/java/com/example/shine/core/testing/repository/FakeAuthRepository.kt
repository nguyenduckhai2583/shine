package com.example.shine.core.testing.repository

import com.example.shine.core.data.repository.AuthRepository
import com.example.shine.core.model.Session
import com.example.shine.core.model.SignInStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository(
    initialSession: Session? = null,
) : AuthRepository {

    val sessionFlow = MutableStateFlow<Session?>(initialSession)
    override val session: StateFlow<Session?> = sessionFlow

    var signInResult: Result<SignInStatus> = Result.success(
        SignInStatus.Authenticated(Session(token = "token", workspaceId = "test-ws")),
    )
    var nextError: Throwable? = null
    var signOutCalled: Boolean = false
    var signedOut: Boolean
        get() = signOutCalled
        set(value) {
            signOutCalled = value
        }
    var lastSignInEmail: String? = null
    var lastEmail: String?
        get() = lastSignInEmail
        set(value) {
            lastSignInEmail = value
        }
    var lastSignInPassword: String? = null

    override suspend fun signIn(email: String, password: String): Result<SignInStatus> {
        lastSignInEmail = email
        lastSignInPassword = password
        nextError?.let { return Result.failure(it) }
        val currentSession = sessionFlow.value ?: Session("token", null, null, null)
        sessionFlow.value = currentSession
        return if (signInResult.isSuccess) {
            Result.success(SignInStatus.Authenticated(currentSession))
        } else {
            signInResult
        }
    }

    override suspend fun signOut() {
        signOutCalled = true
        sessionFlow.value = null
    }
}
