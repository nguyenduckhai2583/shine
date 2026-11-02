package com.example.shine.data.remote.socket

import android.util.Log
import com.example.shine.data.remote.socket.client.SocketClient
import com.example.shine.data.remote.socket.event.SocketEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketServiceImpl @Inject constructor(
    private val socketClient: SocketClient,
    private val deduplicator: SocketEventDeduplicator,
    private val json: Json,
) : SocketService {

    private val _connectionState = MutableStateFlow(SocketConnectionState.Disconnected)
    override val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    override val events: Flow<SocketEvent> = _events.asSharedFlow()

    /** Single source of truth for active credentials and room subscriptions */
    private var activeSession: SocketSessionConfig? = null

    override val isJoinedRoom: Boolean
        get() = (connectionState.value.isConnected && (activeSession?.userId != null))

    override val joinedWorkspaceId: String?
        get() = if (connectionState.value.isConnected) activeSession?.workspaceId else null

    private fun updateConnectionState(newState: SocketConnectionState) {
        _connectionState.update { oldState ->
            if (oldState != newState) {
                Log.d(TAG, "🟢 [SOCKET] State changed: $oldState ──► $newState")
            }
            newState
        }
    }

    override fun connectToRoom(token: String, userId: String?, workspaceId: String?) {
        if (token.isBlank()) return
        activeSession = SocketSessionConfig(token = token, userId = userId, workspaceId = workspaceId)
        updateConnectionState(SocketConnectionState.Connecting)

        socketClient.connect(token) { state ->
            updateConnectionState(state)
            if (state.isConnected) {
                syncRooms()
                registerEventHandlers()
            }
        }
    }

    override fun reconnect(token: String, userId: String?, workspaceId: String?) {
        if (token.isBlank()) return
        val previousSession = activeSession
        if (previousSession == null) {
            connectToRoom(token, userId, workspaceId)
            return
        }

        activeSession = SocketSessionConfig(token = token, userId = userId, workspaceId = workspaceId)

        val tokenChanged = previousSession.token != token
        if (!socketClient.isConnected() || tokenChanged) {
            updateConnectionState(SocketConnectionState.Connecting)
            socketClient.reconnect(token)
        } else if ((previousSession.workspaceId != workspaceId) && (workspaceId != null)) {
            // Restore previousSession.workspaceId so joinWorkspaceRoom can leave the old one
            activeSession = activeSession?.copy(workspaceId = previousSession.workspaceId)
            joinWorkspaceRoom(workspaceId)
        }
    }

    override fun switchTo(token: String, userId: String?, workspaceId: String?) {
        if (token.isBlank()) return
        activeSession = null
        deduplicator.clear()
        socketClient.reset()
        connectToRoom(token = token, userId = userId, workspaceId = workspaceId)
    }

    override fun joinWorkspaceRoom(workspaceId: String) {
        val oldId = activeSession?.workspaceId
        activeSession = activeSession?.copy(workspaceId = workspaceId)
        if (!isJoinedRoom) return
        if ((oldId != null) && (oldId != workspaceId)) {
            leaveWorkspaceRoom(oldId)
        }
        socketClient.emit(
            SocketConstants.EVENT_JOIN_ROOM,
            SocketRoom.payload(SocketRoom.workspace(workspaceId)),
        )
    }

    override fun leaveWorkspaceRoom(workspaceId: String) {
        if (!isJoinedRoom) return
        socketClient.emit(
            SocketConstants.EVENT_LEFT_ROOM,
            SocketRoom.payload(SocketRoom.workspace(workspaceId)),
        )
        if (activeSession?.workspaceId == workspaceId) {
            activeSession = activeSession?.copy(workspaceId = null)
        }
    }

    override fun leftRoom() {
        if (!isJoinedRoom) return
        socketClient.emit(SocketConstants.EVENT_LEFT_ROOM, emptyMap<String, String>())
        activeSession = null
        deduplicator.clear()
    }

    override fun disconnect() {
        socketClient.disconnect()
        updateConnectionState(SocketConnectionState.Disconnected)
        // Note: deduplicator cache is deliberately preserved across transport reconnections
    }

    override fun emit(event: String, payload: Map<String, Any?>?) {
        socketClient.emit(event, payload)
    }

    private fun syncRooms() {
        val (_, userId, workspaceId) = activeSession ?: return
        if (!userId.isNullOrEmpty()) {
            socketClient.emit(
                SocketConstants.EVENT_JOIN_ROOM,
                SocketRoom.payload(SocketRoom.user(userId)),
            )
        }
        if (!workspaceId.isNullOrEmpty()) {
            socketClient.emit(
                SocketConstants.EVENT_JOIN_ROOM,
                SocketRoom.payload(SocketRoom.workspace(workspaceId)),
            )
        }
    }

    private fun registerEventHandlers() {
        for (eventType in SocketEvent.supportedEventTypes) {
            socketClient.off(eventType)
            socketClient.on(eventType) { args ->
                dispatchEvent(eventType, args)
            }
        }
    }

    internal fun dispatchEvent(eventType: String, args: Array<Any>) {
        if (args.isEmpty()) return

        val (payload, eventId) = extractPayloadAndEventId(args) ?: return

        if (deduplicator.isDuplicate(eventId)) {
            Log.d(TAG, "🟢 [SOCKET] Duplicate event dropped: $eventType ($eventId)")
            return
        }

        try {
            val jsonObject = when (payload) {
                is JsonObject -> payload
                else -> json.parseToJsonElement(payload.toString()).jsonObject
            }
            val parsedEvent = SocketEvent.parse(eventType, jsonObject)
            Log.d(TAG, "🟢 [SOCKET] New event: $eventType -> $parsedEvent")
            parsedEvent?.let { _events.tryEmit(it) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ [SOCKET] Parse error [$eventType]: ${e.message}", e)
        }
    }

    private fun extractPayloadAndEventId(args: Array<Any>): Pair<Any, String?>? {
        val payload = args.firstOrNull() ?: return null
        val eventId = args.getOrNull(1) as? String
        return payload to eventId
    }

    companion object {
        private const val TAG = "SOCKET_DEBUG"
    }
}
