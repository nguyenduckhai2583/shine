package com.example.shine.core.data.session

import com.example.shine.core.data.sync.ChannelSyncScheduler
import com.example.shine.core.database.ShineDatabase
import com.example.shine.core.datastore.DirectMessageDataStore
import com.example.shine.core.datastore.SessionLocalDataSource
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
class SessionManagerTest {

    private val sessionFlow = MutableStateFlow<Session?>(null)
    private lateinit var sessionLocal: SessionLocalDataSource
    private lateinit var database: ShineDatabase
    private lateinit var inMemoryDatabase: ShineDatabase
    private lateinit var directMessageDataStore: DirectMessageDataStore
    private lateinit var channelSyncScheduler: ChannelSyncScheduler
    private lateinit var sessionManager: SessionManager

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = CoroutineScope(testDispatcher)

    @Before
    fun setUp() {
        sessionLocal = mock(SessionLocalDataSource::class.java)
        `when`(sessionLocal.session).thenReturn(sessionFlow)

        database = mock(ShineDatabase::class.java)
        inMemoryDatabase = mock(ShineDatabase::class.java)
        directMessageDataStore = mock(DirectMessageDataStore::class.java)
        channelSyncScheduler = mock(ChannelSyncScheduler::class.java)

        sessionManager = SessionManager(
            sessionLocalDataSource = sessionLocal,
            database = database,
            inMemoryDatabase = inMemoryDatabase,
            directMessageDataStore = directMessageDataStore,
            channelSyncScheduler = channelSyncScheduler,
            scope = testScope,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun initialSignIn_doesNotTriggerCleanup() = runTest(testDispatcher) {
        sessionManager.start()
        sessionFlow.value = Session(token = "token-1")

        verify(channelSyncScheduler, never()).cancelSync()
        verify(database, never()).clearAll()
    }

    @Test
    fun logout_triggersAutomaticCleanup() = runTest(testDispatcher) {
        sessionManager.start()
        sessionFlow.value = Session(token = "token-1")

        sessionFlow.value = null

        verify(channelSyncScheduler).cancelSync()
        verify(database).clearAll()
        verify(inMemoryDatabase).clearAll()
        verify(directMessageDataStore).clear()
    }

    @Test
    fun switchUser_triggersAutomaticCleanup() = runTest(testDispatcher) {
        sessionManager.start()
        sessionFlow.value = Session(token = "token-1")

        sessionFlow.value = Session(token = "token-2")

        verify(channelSyncScheduler).cancelSync()
        verify(database).clearAll()
        verify(inMemoryDatabase).clearAll()
        verify(directMessageDataStore).clear()
    }
}
