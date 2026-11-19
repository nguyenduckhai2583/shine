package com.example.shine.core.data.session

import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.model.AuthState
import com.example.shine.core.model.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    private val sessionFlow = MutableStateFlow<Session?>(null)
    private lateinit var sessionLocal: SessionLocalDataSource
    private lateinit var sessionManager: SessionManager

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = CoroutineScope(testDispatcher)

    @Before
    fun setUp() {
        sessionLocal = mock(SessionLocalDataSource::class.java)
        `when`(sessionLocal.session).thenReturn(sessionFlow)

        sessionManager = SessionManager(
            sessionLocalDataSource = sessionLocal,
            scope = testScope,
        )
    }

    @Test
    fun nullSession_emitsUnauthenticated() = runTest(testDispatcher) {
        sessionFlow.value = null
        assertEquals(AuthState.Unauthenticated, sessionManager.authState.value)
    }

    @Test
    fun sessionWithoutWorkspace_emitsSelectWorkspace() = runTest(testDispatcher) {
        sessionFlow.value = Session(token = "token-1", workspaceId = null)
        assertEquals(AuthState.SelectWorkspace, sessionManager.authState.value)
    }

    @Test
    fun sessionWithWorkspace_emitsAuthenticated() = runTest(testDispatcher) {
        sessionFlow.value = Session(token = "token-1", workspaceId = "ws-1")
        assertEquals(AuthState.Authenticated, sessionManager.authState.value)
    }
}
