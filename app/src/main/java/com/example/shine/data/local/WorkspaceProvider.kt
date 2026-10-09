package com.example.shine.data.local

import com.example.shine.core.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the latest workspace ID in memory so OkHttp can read it synchronously. */
@Singleton
class WorkspaceProvider @Inject constructor(
    private val local: SessionLocalDataSource,
    @ApplicationScope scope: CoroutineScope,
) {
    private val workspaceId: StateFlow<String?> = local.session
        .map { it?.workspaceId }
        .stateIn(scope, SharingStarted.Eagerly, null)

    fun current(): String? = workspaceId.value ?: runBlocking { local.session.first()?.workspaceId }
}
