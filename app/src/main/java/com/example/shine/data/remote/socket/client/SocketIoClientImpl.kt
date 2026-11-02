package com.example.shine.data.remote.socket.client

import android.util.Log
import com.example.shine.data.remote.socket.SocketConnectionState
import com.example.shine.data.remote.socket.SocketConstants
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketIoClientImpl @Inject constructor() : SocketClient {

    private var socket: Socket? = null
    private var baseUrl: String? = null
    private val authPayload = mutableMapOf<String, String>()
    private var stateChangedListener: ((SocketConnectionState) -> Unit)? = null

    override fun init(baseUrl: String) {
        val cleanUrl = baseUrl.trimEnd('/')
        this.baseUrl = cleanUrl
        createSocket(cleanUrl)
    }

    private fun createSocket(cleanUrl: String) {
        try {
            val options = IO.Options().apply {
                transports = arrayOf(WebSocket.NAME)
                path = SocketConstants.SOCKET_PATH
                forceNew = true
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000L
                reconnectionDelayMax = 5000L
                timeout = 8000L
                auth = authPayload
            }
            val newSocket = IO.socket(URI.create(cleanUrl), options)
            wireLifecycleHandlers(newSocket)
            socket = newSocket
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing socket.io: ${e.message}", e)
        }
    }

    override fun connect(token: String, onStateChanged: (SocketConnectionState) -> Unit) {
        this.stateChangedListener = onStateChanged
        authPayload["token"] = token

        val url = baseUrl ?: return
        if (socket == null) {
            createSocket(url)
        }

        val currentSocket = socket ?: return
        if (!currentSocket.connected()) {
            currentSocket.connect()
        }
    }

    override fun reconnect(token: String) {
        authPayload["token"] = token
        val currentSocket = socket
        if (currentSocket != null) {
            if (!currentSocket.connected()) {
                currentSocket.connect()
            }
        } else {
            val url = baseUrl ?: return
            createSocket(url)
            socket?.connect()
        }
    }

    private fun wireLifecycleHandlers(socket: Socket) {
        socket.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "🟢 [SOCKET] CONNECT SUCCESS (id: ${socket.id()})")
            stateChangedListener?.invoke(SocketConnectionState.Connected)
        }

        socket.on(Socket.EVENT_DISCONNECT) { args ->
            val reason = args.firstOrNull()?.toString() ?: "unknown"
            Log.d(TAG, "🟢 [SOCKET] DISCONNECTED (reason: $reason)")
            stateChangedListener?.invoke(SocketConnectionState.Disconnected)
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val error = args.firstOrNull()
            Log.d(TAG, "🟢 [SOCKET] CONNECT ERROR: $error", error as? Throwable)
            stateChangedListener?.invoke(SocketConnectionState.Disconnected)
        }

        socket.on(SocketConstants.EVENT_JOIN_ROOM) { args ->
            Log.d(TAG, "🟢 [SOCKET] Server ack JOIN_ROOM: ${args?.contentToString()}")
        }

        socket.on(SocketConstants.EVENT_ERROR) { args ->
            Log.e(TAG, "❌ [SOCKET] Server ERROR: ${args?.contentToString()}")
        }

        socket.on(SocketConstants.EVENT_EXCEPTION) { args ->
            Log.e(TAG, "❌ [SOCKET] Server EXCEPTION: ${args?.contentToString()}")
        }
    }

    override fun isConnected(): Boolean = socket?.connected() == true

    override fun emit(event: String, payload: Map<String, Any?>?) {
        Log.d(TAG, "🟢 [SOCKET] Emit event: $event, payload: $payload")
        if (payload == null) {
            socket?.emit(event)
        } else {
            socket?.emit(event, JSONObject(payload))
        }
    }

    override fun on(event: String, listener: (Array<Any>) -> Unit) {
        socket?.on(event) { args -> listener(args ?: emptyArray()) }
    }

    override fun off(event: String, listener: ((Array<Any>) -> Unit)?) {
        socket?.off(event)
    }

    override fun disconnect() {
        socket?.disconnect()
    }

    override fun reset() {
        socket?.disconnect()
        socket?.close()
        socket = null
        authPayload.clear()
        baseUrl?.let { createSocket(it) }
    }

    companion object {
        private const val TAG = "SOCKET_DEBUG"
    }
}
