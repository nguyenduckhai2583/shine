package com.example.shine.core.data.session

import com.example.shine.core.common.di.ApplicationScope
import com.example.shine.core.common.di.IoDispatcher
import com.example.shine.core.data.sync.ChannelSyncScheduler
import com.example.shine.core.database.ShineDatabase
import com.example.shine.core.database.di.InMemoryDatabase
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.model.AuthState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ensures data integrity across the entire application lifecycle.
 * Automatically cleans up local databases, in-memory caches, and background sync workers
 * when the user session is invalidated (transitions to [AuthState.Unauthenticated]).
 */
@Singleton
class SessionCleaner @Inject constructor(
    private val sessionManager: SessionManager,
    private val database: ShineDatabase,
    @InMemoryDatabase private val inMemoryDatabase: ShineDatabase,
    private val channelSyncScheduler: ChannelSyncScheduler,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch(ioDispatcher) {
            var previousState: AuthState? = null
            sessionManager.authState.collect { currentState ->
                val wasActive =
                    previousState == AuthState.Authenticated || previousState == AuthState.SelectWorkspace
                if (wasActive && currentState == AuthState.Unauthenticated) {
                    performCleanup()
                }
                previousState = currentState
            }
        }
    }

    fun performCleanup() {
        channelSyncScheduler.cancelSync()
        database.clearAll()
        inMemoryDatabase.clearAll()
    }
}
