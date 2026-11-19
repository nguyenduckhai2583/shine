package com.example.shine.core.datastore

import com.example.shine.core.common.di.ApplicationScope
import com.example.shine.core.model.AuthState
import com.example.shine.core.model.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for in-memory session, current workspace, and application authentication state.
 * Observes [SessionLocalDataSource] and exposes reactive state flows and synchronous getters.
 */
@Singleton
class SessionManager @Inject constructor(
    private val sessionLocalDataSource: SessionLocalDataSource,
    @ApplicationScope scope: CoroutineScope,
) {
    val session: StateFlow<Session?> = sessionLocalDataSource.session
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    val workspaceId: Flow<String?> = sessionLocalDataSource.session
        .map { it?.workspaceId }
        .distinctUntilChanged()

    val authState: StateFlow<AuthState> = sessionLocalDataSource.session
        .map { currentSession ->
            when {
                currentSession == null -> AuthState.Unauthenticated
                currentSession.workspaceId.isNullOrEmpty() -> AuthState.SelectWorkspace
                else -> AuthState.Authenticated
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = AuthState.Initializing,
        )

    fun currentToken(): String? = session.value?.token ?: runBlocking { sessionLocalDataSource.session.first()?.token }

    fun currentWorkspaceId(): String? = session.value?.workspaceId ?: runBlocking { sessionLocalDataSource.session.first()?.workspaceId }

    fun currentUserId(): String? = session.value?.user?.id ?: runBlocking { sessionLocalDataSource.session.first()?.user?.id }

    fun currentSession(): Session? = session.value ?: runBlocking { sessionLocalDataSource.session.first() }

    suspend fun getSession(): Session? = session.value ?: sessionLocalDataSource.session.first()
}
