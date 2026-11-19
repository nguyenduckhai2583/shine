package com.example.shine.core.data.session

import com.example.shine.core.common.di.ApplicationScope
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.model.AuthState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for application authentication state.
 * Observes [SessionLocalDataSource] and exposes [authState] StateFlow.
 */
@Singleton
class SessionManager @Inject constructor(
    sessionLocalDataSource: SessionLocalDataSource,
    @ApplicationScope scope: CoroutineScope,
) {
    val authState: StateFlow<AuthState> = sessionLocalDataSource.session
        .map { session ->
            when {
                session == null -> AuthState.Unauthenticated
                session.workspaceId.isNullOrEmpty() -> AuthState.SelectWorkspace
                else -> AuthState.Authenticated
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = AuthState.Initializing,
        )
}
