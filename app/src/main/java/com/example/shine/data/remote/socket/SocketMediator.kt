package com.example.shine.data.remote.socket

import com.example.shine.data.remote.socket.event.SocketEvent

/**
 * Common interface for domain socket mediators that process global socket events
 * and dispatch them to repositories/datastores.
 *
 * Mediators are collected automatically via Dagger Multibindings into [SocketLifecycleManager].
 */
interface SocketMediator {
    suspend fun handleEvent(event: SocketEvent)
}

