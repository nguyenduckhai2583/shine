package com.example.shine.data.remote.socket

import com.example.shine.core.util.AppLifecycleMonitor
import com.example.shine.core.util.NetworkMonitor
import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.remote.socket.event.ConversationCreatedEvent
import com.example.shine.data.remote.socket.event.SocketEvent
import com.example.shine.data.remote.socket.model.ConvSocketMessage
import com.example.shine.data.remote.socket.model.ConversationType
import com.example.shine.domain.model.Session
import com.example.shine.domain.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class SocketLifecycleManagerTest {

    private class FakeSocketService : SocketService {
        var lastToken: String? = null
        var lastUserId: String? = null
        var lastWorkspaceId: String? = null
        var disconnectCalled = false
        var reconnectCalled = false
        var lastJoinedWorkspaceRoom: String? = null

        val mutableConnectionState = MutableStateFlow(SocketConnectionState.Disconnected)
        override val connectionState: StateFlow<SocketConnectionState> = mutableConnectionState

        val eventsFlow = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 16)
        override val events: Flow<SocketEvent> = eventsFlow
        override val isJoinedRoom: Boolean = false
        override var joinedWorkspaceId: String? = null

        override fun connectToRoom(token: String, userId: String?, workspaceId: String?) {
            lastToken = token
            lastUserId = userId
            lastWorkspaceId = workspaceId
        }

        override fun switchTo(token: String, userId: String?, workspaceId: String?) {}

        override fun joinWorkspaceRoom(workspaceId: String) {
            lastJoinedWorkspaceRoom = workspaceId
            joinedWorkspaceId = workspaceId
        }

        override fun leaveWorkspaceRoom(workspaceId: String) {}
        override fun leftRoom() {}

        override fun disconnect() {
            disconnectCalled = true
            mutableConnectionState.value = SocketConnectionState.Disconnected
        }

        override fun emit(event: String, payload: Map<String, Any?>?) {}

        override fun reconnect(token: String, userId: String?, workspaceId: String?) {
            reconnectCalled = true
            lastToken = token
            lastUserId = userId
            lastWorkspaceId = workspaceId
        }
    }

    private lateinit var sessionLocal: SessionLocalDataSource
    private lateinit var socketService: FakeSocketService
    private lateinit var mediator: SocketMediator
    private val sessionFlow = MutableSharedFlow<Session?>(replay = 1)
    private val isInForegroundFlow = MutableStateFlow(true)
    private val isOnlineFlow = MutableStateFlow(true)

    private val appLifecycleMonitor = object : AppLifecycleMonitor {
        override val isInForeground: Flow<Boolean> = isInForegroundFlow
    }

    private val networkMonitor = object : NetworkMonitor {
        override val isOnline: Flow<Boolean> = isOnlineFlow
    }

    @Before
    fun setUp() {
        sessionLocal = mock(SessionLocalDataSource::class.java)
        socketService = FakeSocketService()
        mediator = mock(SocketMediator::class.java)
        `when`(sessionLocal.session).thenReturn(sessionFlow)
        isInForegroundFlow.value = true
        isOnlineFlow.value = true
    }

    @Test
    fun start_whenValidSessionAndForegroundAndOnline_reconnectsSocket() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val manager = SocketLifecycleManager(
            sessionLocalDataSource = sessionLocal,
            appLifecycleMonitor = appLifecycleMonitor,
            networkMonitor = networkMonitor,
            socketService = socketService,
            mediators = setOf(mediator),
            scope = this,
            ioDispatcher = testDispatcher,
        )
        manager.start()

        sessionFlow.emit(
            Session(
                token = "token-abc",
                user = User(id = "user-123"),
                workspaceId = "ws-456",
            )
        )
        advanceUntilIdle()

        assertTrue(socketService.reconnectCalled)
        assertEquals("token-abc", socketService.lastToken)
        assertEquals("user-123", socketService.lastUserId)
        assertEquals("ws-456", socketService.lastWorkspaceId)

        manager.stop()
    }

    @Test
    fun events_whenEmitted_dispatchedToMediators() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val manager = SocketLifecycleManager(
            sessionLocalDataSource = sessionLocal,
            appLifecycleMonitor = appLifecycleMonitor,
            networkMonitor = networkMonitor,
            socketService = socketService,
            mediators = setOf(mediator),
            scope = this,
            ioDispatcher = testDispatcher,
        )
        manager.start()

        val sampleEvent = ConversationCreatedEvent(
            ConvSocketMessage(
                type = ConversationType.CHANNEL,
                workspaceId = "ws-1",
                rawJson = JsonObject(emptyMap()),
            )
        )

        socketService.eventsFlow.emit(sampleEvent)
        advanceUntilIdle()

        verify(mediator).handleEvent(sampleEvent)

        manager.stop()
    }

    @Test
    fun appLifecycle_whenAppGoesToBackground_disconnectsSocket() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val manager = SocketLifecycleManager(
            sessionLocalDataSource = sessionLocal,
            appLifecycleMonitor = appLifecycleMonitor,
            networkMonitor = networkMonitor,
            socketService = socketService,
            mediators = setOf(mediator),
            scope = this,
            ioDispatcher = testDispatcher,
        )
        manager.start()

        sessionFlow.emit(
            Session(
                token = "token-abc",
                user = User(id = "user-123"),
                workspaceId = "ws-456",
            )
        )
        advanceUntilIdle()

        socketService.disconnectCalled = false

        // App goes to background
        isInForegroundFlow.value = false
        advanceUntilIdle()

        assertTrue(socketService.disconnectCalled)

        // App returns to foreground
        socketService.reconnectCalled = false
        isInForegroundFlow.value = true
        advanceUntilIdle()

        assertTrue(socketService.reconnectCalled)

        manager.stop()
    }

    @Test
    fun network_whenDeviceGoesOffline_disconnects_andWhenOnline_reconnects() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val manager = SocketLifecycleManager(
            sessionLocalDataSource = sessionLocal,
            appLifecycleMonitor = appLifecycleMonitor,
            networkMonitor = networkMonitor,
            socketService = socketService,
            mediators = setOf(mediator),
            scope = this,
            ioDispatcher = testDispatcher,
        )
        manager.start()

        sessionFlow.emit(
            Session(
                token = "token-abc",
                user = User(id = "user-123"),
                workspaceId = "ws-456",
            )
        )
        advanceUntilIdle()

        socketService.disconnectCalled = false

        // Network lost
        isOnlineFlow.value = false
        advanceUntilIdle()

        assertTrue(socketService.disconnectCalled)

        // Network restored
        socketService.reconnectCalled = false
        isOnlineFlow.value = true
        advanceUntilIdle()

        assertTrue(socketService.reconnectCalled)

        manager.stop()
    }

    @Test
    fun workspaceSwitch_reconnectsWithNewWorkspace() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val manager = SocketLifecycleManager(
            sessionLocalDataSource = sessionLocal,
            appLifecycleMonitor = appLifecycleMonitor,
            networkMonitor = networkMonitor,
            socketService = socketService,
            mediators = setOf(mediator),
            scope = this,
            ioDispatcher = testDispatcher,
        )
        manager.start()

        sessionFlow.emit(
            Session(
                token = "token-abc",
                user = User(id = "user-123"),
                workspaceId = "ws-1",
            )
        )
        advanceUntilIdle()

        socketService.reconnectCalled = false

        // User switches to ws-2
        sessionFlow.emit(
            Session(
                token = "token-abc",
                user = User(id = "user-123"),
                workspaceId = "ws-2",
            )
        )
        advanceUntilIdle()

        assertTrue(socketService.reconnectCalled)
        assertEquals("ws-2", socketService.lastWorkspaceId)

        manager.stop()
    }

    @Test
    fun start_whenSessionEmitsNull_disconnects() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val manager = SocketLifecycleManager(
            sessionLocalDataSource = sessionLocal,
            appLifecycleMonitor = appLifecycleMonitor,
            networkMonitor = networkMonitor,
            socketService = socketService,
            mediators = setOf(mediator),
            scope = this,
            ioDispatcher = testDispatcher,
        )
        manager.start()

        sessionFlow.emit(null)
        advanceUntilIdle()

        assertTrue(socketService.disconnectCalled)

        manager.stop()
    }
}
