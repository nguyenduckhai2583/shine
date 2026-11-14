package com.example.shine.core.network.socket

enum class SocketConnectionState {
    Connecting,
    Connected,
    Disconnected;

    val isConnected: Boolean get() = this == Connected
}

