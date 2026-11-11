package com.example.shine.core.datastore

import com.example.shine.core.common.di.ApplicationScope
import com.example.shine.core.model.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the latest session in memory as an eager StateFlow so that
 * OkHttp interceptors and other background components can read token and workspaceId synchronously
 * without blocking disk I/O on every request.
 */
@Singleton
class SessionProvider @Inject constructor(
    private val local: SessionLocalDataSource,
    @ApplicationScope scope: CoroutineScope,
) {
    val session: StateFlow<Session?> = local.session
        .stateIn(scope, SharingStarted.Eagerly, null)

    fun currentToken(): String? = session.value?.token ?: runBlocking { local.session.first()?.token }

    fun currentWorkspaceId(): String? = session.value?.workspaceId ?: runBlocking { local.session.first()?.workspaceId }

    fun currentSession(): Session? = session.value ?: runBlocking { local.session.first() }
}
