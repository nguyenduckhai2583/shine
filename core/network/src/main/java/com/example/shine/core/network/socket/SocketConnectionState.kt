package com.example.shine.core.network.socket

enum class SocketConnectionState {
    Connecting,
    Connected,
    Disconnected;

    val isConnected: Boolean get() = this == Connected
    val isConnecting: Boolean get() = this == Connecting
    val isDisconnected: Boolean get() = this == Disconnected
}

val SocketService.isConnected: Boolean
    get() = connectionState.value.isConnected

val SocketService.isConnecting: Boolean
    get() = connectionState.value.isConnecting

val SocketService.isDisconnected: Boolean
    get() = connectionState.value.isDisconnected
