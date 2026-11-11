package com.example.shine.core.network.socket

import com.example.shine.core.network.socket.client.SocketClient
import com.example.shine.core.network.socket.event.ConversationCreatedEvent
import com.example.shine.core.network.socket.event.SocketEvent
import com.example.shine.core.network.socket.model.ConversationType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SocketServiceImplTest {

    private class FakeSocketClient : SocketClient {
        var connected = false
        var lastConnectedToken: String? = null
        var stateChangedCallback: ((SocketConnectionState) -> Unit)? = null

        val emittedEvents = mutableListOf<Pair<String, Map<String, Any?>?>>()
        val listeners = mutableMapOf<String, (Array<Any>) -> Unit>()
        var resetCalled = false
        var disconnectCalled = false

        var reconnectCalled = false
        var lastReconnectedToken: String? = null

        override fun init(baseUrl: String) {}

        override fun connect(token: String, onStateChanged: (SocketConnectionState) -> Unit) {
            lastConnectedToken = token
            stateChangedCallback = onStateChanged
        }

        override fun reconnect(token: String) {
            reconnectCalled = true
            lastReconnectedToken = token
        }

        override fun isConnected(): Boolean = connected

        override fun emit(event: String, payload: Map<String, Any?>?) {
            emittedEvents.add(event to payload)
        }

        override fun on(event: String, listener: (Array<Any>) -> Unit) {
            listeners[event] = listener
        }

        override fun off(event: String, listener: ((Array<Any>) -> Unit)?) {
            listeners.remove(event)
        }

        override fun disconnect() {
            connected = false
            disconnectCalled = true
        }

        override fun reset() {
            resetCalled = true
            connected = false
            listeners.clear()
        }
    }

    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var client: FakeSocketClient
    private lateinit var deduplicator: SocketEventDeduplicatorImpl
    private lateinit var service: SocketServiceImpl

    @Before
    fun setUp() {
        client = FakeSocketClient()
        deduplicator = SocketEventDeduplicatorImpl()
        service = SocketServiceImpl(
            socketClient = client,
            deduplicator = deduplicator,
            json = json,
        )
    }

    @Test
    fun connectToRoom_onConnected_joinsUserAndWorkspaceRooms() {
        service.connectToRoom(token = "jwt-123", userId = "user-1", workspaceId = "ws-1")

        assertEquals(SocketConnectionState.Connecting, service.connectionState.value)
        assertEquals("jwt-123", client.lastConnectedToken)

        // Trigger connected callback
        client.stateChangedCallback?.invoke(SocketConnectionState.Connected)

        assertEquals(SocketConnectionState.Connected, service.connectionState.value)
        assertTrue(service.isJoinedRoom)
        assertEquals("ws-1", service.joinedWorkspaceId)

        // Verify user room and workspace room joined
        assertEquals(2, client.emittedEvents.size)
        assertEquals(SocketConstants.EVENT_JOIN_ROOM, client.emittedEvents[0].first)
        assertEquals(mapOf("room" to "user.user-1"), client.emittedEvents[0].second)

        assertEquals(SocketConstants.EVENT_JOIN_ROOM, client.emittedEvents[1].first)
        assertEquals(mapOf("room" to "workspace.ws-1"), client.emittedEvents[1].second)

        // Verify listeners registered
        assertTrue(client.listeners.containsKey(SocketConstants.EVENT_CONVERSATION_CREATED))
    }

    @Test
    fun joinWorkspaceRoom_whenSwitchingWorkspace_leavesOldAndJoinsNewRoom() {
        service.connectToRoom(token = "jwt-123", userId = "user-1", workspaceId = "ws-1")
        client.stateChangedCallback?.invoke(SocketConnectionState.Connected)
        client.emittedEvents.clear()

        service.joinWorkspaceRoom("ws-2")

        assertEquals("ws-2", service.joinedWorkspaceId)
        assertEquals(2, client.emittedEvents.size)
        // Leave old
        assertEquals(SocketConstants.EVENT_LEFT_ROOM, client.emittedEvents[0].first)
        assertEquals(mapOf("room" to "workspace.ws-1"), client.emittedEvents[0].second)
        // Join new
        assertEquals(SocketConstants.EVENT_JOIN_ROOM, client.emittedEvents[1].first)
        assertEquals(mapOf("room" to "workspace.ws-2"), client.emittedEvents[1].second)
    }

    @Test
    fun switchTo_resetsClientAndReconnects() {
        service.connectToRoom(token = "token-1", userId = "u1", workspaceId = "ws-1")
        client.stateChangedCallback?.invoke(SocketConnectionState.Connected)

        service.switchTo(token = "token-2", userId = "u2", workspaceId = "ws-2")

        assertTrue(client.resetCalled)
        assertEquals("token-2", client.lastConnectedToken)
    }

    @Test
    fun leftRoom_emitsLeftRoomAndResetsState() {
        service.connectToRoom(token = "token-1", userId = "u1", workspaceId = "ws-1")
        client.stateChangedCallback?.invoke(SocketConnectionState.Connected)
        client.emittedEvents.clear()

        service.leftRoom()

        assertFalse(service.isJoinedRoom)
        assertEquals(null, service.joinedWorkspaceId)
        assertEquals(1, client.emittedEvents.size)
        assertEquals(SocketConstants.EVENT_LEFT_ROOM, client.emittedEvents[0].first)
    }

    @Test
    fun deduplication_dropsDuplicateEventIdWithinTtl() = runTest {
        val receivedEvents = mutableListOf<SocketEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            service.events.toList(receivedEvents)
        }

        val jsonPayload = """{"type":"CHANNEL","workspaceId":"ws-1","id":"c1","name":"General"}"""

        // First arrival with eventId "evt-1"
        service.dispatchEvent(
            SocketConstants.EVENT_CONVERSATION_CREATED,
            arrayOf(jsonPayload, "evt-1"),
        )
        assertEquals(1, receivedEvents.size)

        // Duplicate arrival with same eventId "evt-1"
        service.dispatchEvent(
            SocketConstants.EVENT_CONVERSATION_CREATED,
            arrayOf(jsonPayload, "evt-1"),
        )
        // Should be dropped
        assertEquals(1, receivedEvents.size)

        // Different eventId "evt-2" passes through
        service.dispatchEvent(
            SocketConstants.EVENT_CONVERSATION_CREATED,
            arrayOf(jsonPayload, "evt-2"),
        )
        assertEquals(2, receivedEvents.size)

        // Frame without eventId passes through
        service.dispatchEvent(
            SocketConstants.EVENT_CONVERSATION_CREATED,
            arrayOf(jsonPayload),
        )
        assertEquals(3, receivedEvents.size)

        collectJob.cancel()
    }

    @Test
    fun deduplication_leavingRoomClearsSeenIds() = runTest {
        val receivedEvents = mutableListOf<SocketEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            service.events.toList(receivedEvents)
        }

        service.connectToRoom("token-1", "u1", "ws-1")
        client.stateChangedCallback?.invoke(SocketConnectionState.Connected)

        val jsonPayload = """{"type":"CHANNEL","id":"c1","name":"General"}"""

        service.dispatchEvent(
            SocketConstants.EVENT_CONVERSATION_CREATED,
            arrayOf(jsonPayload, "evt-1"),
        )
        assertEquals(1, receivedEvents.size)

        // Leave room
        service.leftRoom()

        // Same eventId "evt-1" arrives after leaving room -> passes through
        service.dispatchEvent(
            SocketConstants.EVENT_CONVERSATION_CREATED,
            arrayOf(jsonPayload, "evt-1"),
        )
        assertEquals(2, receivedEvents.size)

        collectJob.cancel()
    }

    @Test
    fun dispatchEvent_payloadWithEventId_dispatchesCorrectly() = runTest {
        val receivedEvents = mutableListOf<SocketEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            service.events.toList(receivedEvents)
        }

        val jsonPayload = """{"type":"CHANNEL","id":"c1","name":"Random"}"""

        service.dispatchEvent(
            SocketConstants.EVENT_CONVERSATION_CREATED,
            arrayOf(jsonPayload, "evt-array-1"),
        )

        assertEquals(1, receivedEvents.size)
        val event = receivedEvents[0] as ConversationCreatedEvent
        assertEquals(ConversationType.CHANNEL, event.conversation.type)

        collectJob.cancel()
    }

    @Test
    fun reconnect_callsClientReconnectWithoutReset() {
        service.connectToRoom("token-1", "u1", "ws-1")
        client.stateChangedCallback?.invoke(SocketConnectionState.Connected)
        client.connected = false // Connection drops

        service.reconnect("token-1", "u1", "ws-1")

        assertTrue(client.reconnectCalled)
        assertEquals("token-1", client.lastReconnectedToken)
        assertFalse(client.resetCalled)
    }

    @Test
    fun reconnect_whenTokenRefreshedWhileConnected_reconnectsClientWithNewToken() {
        service.connectToRoom("token-old", "u1", "ws-1")
        client.stateChangedCallback?.invoke(SocketConnectionState.Connected)
        client.connected = true
        client.reconnectCalled = false

        // Token refreshed from BE (new token emitted)
        service.reconnect("token-new", "u1", "ws-1")

        assertTrue(client.reconnectCalled)
        assertEquals("token-new", client.lastReconnectedToken)
    }

    @Test
    fun reconnect_whenConnectedAndOnlyWorkspaceChanges_switchesWorkspaceRoom() {
        service.connectToRoom("token-1", "u1", "ws-1")
        client.stateChangedCallback?.invoke(SocketConnectionState.Connected)
        client.connected = true
        client.emittedEvents.clear()
        client.reconnectCalled = false

        // Only workspace changed
        service.reconnect("token-1", "u1", "ws-2")

        assertFalse(client.reconnectCalled)
        assertEquals(2, client.emittedEvents.size)
        assertEquals(SocketConstants.EVENT_LEFT_ROOM, client.emittedEvents[0].first)
        assertEquals(mapOf("room" to "workspace.ws-1"), client.emittedEvents[0].second)
        assertEquals(SocketConstants.EVENT_JOIN_ROOM, client.emittedEvents[1].first)
        assertEquals(mapOf("room" to "workspace.ws-2"), client.emittedEvents[1].second)
    }

    @Test
    fun disconnect_preservesSeenEventIds_soReplayedEventsFromRecoveryAreDropped() = runTest {
        val receivedEvents = mutableListOf<SocketEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            service.events.toList(receivedEvents)
        }

        service.connectToRoom("token-1", "u1", "ws-1")
        client.stateChangedCallback?.invoke(SocketConnectionState.Connected)

        val jsonPayload = """{"type":"CHANNEL","id":"c1","name":"General"}"""

        // Event arrived before disconnect
        service.dispatchEvent(
            SocketConstants.EVENT_CONVERSATION_CREATED,
            arrayOf(jsonPayload, "evt-recovery-1"),
        )
        assertEquals(1, receivedEvents.size)

        // Transport disconnects (e.g. app in background or momentary network drop)
        service.disconnect()

        // Reconnects via same-instance reconnect
        service.reconnect("token-1", "u1", "ws-1")
        client.stateChangedCallback?.invoke(SocketConnectionState.Connected)

        // BE replays "evt-recovery-1" from recovery buffer
        service.dispatchEvent(
            SocketConstants.EVENT_CONVERSATION_CREATED,
            arrayOf(jsonPayload, "evt-recovery-1"),
        )
        // Should be dropped because seenEventIds was preserved!
        assertEquals(1, receivedEvents.size)

        // New event "evt-recovery-2" arrives
        service.dispatchEvent(
            SocketConstants.EVENT_CONVERSATION_CREATED,
            arrayOf(jsonPayload, "evt-recovery-2"),
        )
        assertEquals(2, receivedEvents.size)

        collectJob.cancel()
    }
}
