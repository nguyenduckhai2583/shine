package com.example.shine.data.remote.socket

import com.example.shine.data.remote.socket.event.SocketEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SocketService {
    val connectionState: StateFlow<SocketConnectionState>
    val events: Flow<SocketEvent>
    val isJoinedRoom: Boolean
    val joinedWorkspaceId: String?

    fun connectToRoom(token: String, userId: String? = null, workspaceId: String? = null)
    fun switchTo(token: String, userId: String? = null, workspaceId: String? = null)
    fun joinWorkspaceRoom(workspaceId: String)
    fun leaveWorkspaceRoom(workspaceId: String)
    fun leftRoom()
    fun disconnect()
    fun emit(event: String, payload: Map<String, Any?>? = null)
    fun reconnect(token: String, userId: String? = null, workspaceId: String? = null)
}
