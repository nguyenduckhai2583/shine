package com.example.shine.core.data.session

import com.example.shine.core.data.sync.ChannelSyncScheduler
import com.example.shine.core.database.ShineDatabase
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.model.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class SessionCleanerTest {

    private val sessionFlow = MutableStateFlow<Session?>(null)
    private lateinit var sessionLocal: SessionLocalDataSource
    private lateinit var sessionManager: SessionManager
    private lateinit var database: ShineDatabase
    private lateinit var inMemoryDatabase: ShineDatabase
    private lateinit var channelSyncScheduler: ChannelSyncScheduler
    private lateinit var sessionCleaner: SessionCleaner

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = CoroutineScope(testDispatcher)

    @Before
    fun setUp() {
        sessionLocal = mock(SessionLocalDataSource::class.java)
        `when`(sessionLocal.session).thenReturn(sessionFlow)
        sessionManager = SessionManager(sessionLocal, testScope)

        database = mock(ShineDatabase::class.java)
        inMemoryDatabase = mock(ShineDatabase::class.java)
        channelSyncScheduler = mock(ChannelSyncScheduler::class.java)

        sessionCleaner = SessionCleaner(
            sessionManager = sessionManager,
            database = database,
            inMemoryDatabase = inMemoryDatabase,
            channelSyncScheduler = channelSyncScheduler,
            scope = testScope,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun initialSignIn_doesNotTriggerCleanup() = runTest(testDispatcher) {
        sessionCleaner.start()
        sessionFlow.value = Session(token = "token-1", workspaceId = "ws-1")

        verify(channelSyncScheduler, never()).cancelSync()
        verify(database, never()).clearAll()
    }

    @Test
    fun logout_triggersAutomaticCleanup() = runTest(testDispatcher) {
        sessionCleaner.start()
        sessionFlow.value = Session(token = "token-1", workspaceId = "ws-1")

        sessionFlow.value = null

        verify(channelSyncScheduler).cancelSync()
        verify(database).clearAll()
        verify(inMemoryDatabase).clearAll()
    }

    @Test
    fun logoutFromSelectWorkspace_triggersAutomaticCleanup() = runTest(testDispatcher) {
        sessionCleaner.start()
        sessionFlow.value = Session(token = "token-1", workspaceId = null)

        sessionFlow.value = null

        verify(channelSyncScheduler).cancelSync()
        verify(database).clearAll()
        verify(inMemoryDatabase).clearAll()
    }
}
