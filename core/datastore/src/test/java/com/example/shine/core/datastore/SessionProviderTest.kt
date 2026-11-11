package com.example.shine.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.shine.core.model.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SessionProviderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(testDispatcher)
    private lateinit var local: SessionLocalDataSource
    private lateinit var provider: SessionProvider

    @Before
    fun setUp() {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { tmp.newFile("test_session.preferences_pb") },
        )
        local = SessionLocalDataSource(dataStore, Json)
        provider = SessionProvider(local, scope)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun currentTokenAndWorkspaceId_initiallyNull() {
        assertNull(provider.currentToken())
        assertNull(provider.currentWorkspaceId())
        assertNull(provider.currentSession())
    }

    @Test
    fun currentTokenAndWorkspaceId_reflectSavedSession() = runTest(testDispatcher) {
        val session = Session(token = "tok-123", workspaceId = "ws-456")
        local.save(session)

        assertEquals("tok-123", provider.currentToken())
        assertEquals("ws-456", provider.currentWorkspaceId())
        assertEquals("tok-123", provider.currentSession()?.token)
    }
}
