package com.example.shine.data.remote.socket.client

import com.example.shine.data.remote.socket.SocketConnectionState

/**
 * Socket client abstraction over the underlying socket.io library.
 * This decouples the business logic and socket service from third-party client details
 * and facilitates unit testing.
 */
interface SocketClient {
    fun init(baseUrl: String)
    fun connect(token: String, onStateChanged: (SocketConnectionState) -> Unit)
    fun reconnect(token: String)
    fun isConnected(): Boolean
    fun emit(event: String, payload: Map<String, Any?>? = null)
    fun on(event: String, listener: (Array<Any>) -> Unit)
    fun off(event: String, listener: ((Array<Any>) -> Unit)? = null)
    fun disconnect()
    fun reset()
}
