package com.example.shine.core.network.socket

import com.example.shine.core.network.socket.event.SocketEvent

/**
 * Common interface for domain socket mediators that process global socket events
 * and dispatch them to repositories/datastores.
 *
 * Mediators are collected automatically via Dagger Multibindings into [SocketLifecycleManager].
 */
interface SocketMediator {
    suspend fun handleEvent(event: SocketEvent)
}

