package com.example.shine.core.data.session

import com.example.shine.core.common.di.ApplicationScope
import com.example.shine.core.common.di.IoDispatcher
import com.example.shine.core.data.sync.ChannelSyncScheduler
import com.example.shine.core.database.ShineDatabase
import com.example.shine.core.database.di.InMemoryDatabase
import com.example.shine.core.datastore.DirectMessageDataStore
import com.example.shine.core.datastore.SessionLocalDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates application-wide session lifecycle.
 * Listens to session changes from [SessionLocalDataSource] and automatically cleans up
 * local database tables, in-memory caches, and background sync workers when the session is invalidated.
 */
@Singleton
class SessionManager @Inject constructor(
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val database: ShineDatabase,
    @InMemoryDatabase private val inMemoryDatabase: ShineDatabase,
    private val directMessageDataStore: DirectMessageDataStore,
    private val channelSyncScheduler: ChannelSyncScheduler,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch(ioDispatcher) {
            var previousToken: String? = null
            sessionLocalDataSource.session.collect { session ->
                val currentToken = session?.token
                if (previousToken != null && previousToken != currentToken) {
                    performCleanup()
                }
                previousToken = currentToken
            }
        }
    }

    fun performCleanup() {
        channelSyncScheduler.cancelSync()
        database.clearAll()
        inMemoryDatabase.clearAll()
        directMessageDataStore.clear()
    }
}
