package com.example.shine.core.datastore

import com.example.shine.core.model.AuthState
import com.example.shine.core.model.Session
import com.example.shine.core.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

    @Test
    fun workspaceId_reflectsSessionWorkspace() = runTest(testDispatcher) {
        sessionFlow.value = Session(token = "t1", workspaceId = "ws-1")
        assertEquals("ws-1", sessionManager.workspaceId.first())

        sessionFlow.value = Session(token = "t1", workspaceId = "ws-2")
        assertEquals("ws-2", sessionManager.workspaceId.first())
    }

    @Test
    fun synchronousGetters_returnCorrectValuesFromInMemoryState() = runTest(testDispatcher) {
        val user = User("u1", "u@example.com", "John", "Doe")
        sessionFlow.value = Session(token = "tok-123", workspaceId = "ws-99", user = user)

        assertEquals("tok-123", sessionManager.currentToken())
        assertEquals("ws-99", sessionManager.currentWorkspaceId())
        assertEquals("u1", sessionManager.currentUserId())
        assertEquals("tok-123", sessionManager.currentSession()?.token)
    }

    @Test
    fun getSession_returnsSessionCorrectly() = runTest(testDispatcher) {
        val user = User("u1", "u@example.com", "John", "Doe")
        sessionFlow.value = Session(token = "tok-123", workspaceId = "ws-99", user = user)

        val session = sessionManager.getSession()
        assertEquals("tok-123", session?.token)
        assertEquals("ws-99", session?.workspaceId)
    }
}
