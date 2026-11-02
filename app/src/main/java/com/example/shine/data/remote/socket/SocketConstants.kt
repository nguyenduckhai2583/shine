package com.example.shine.data.remote.socket

object SocketConstants {
    const val SOCKET_PATH = "/socket.io"

    // Room events
    const val EVENT_JOIN_ROOM = "JOIN_ROOM"
    const val EVENT_LEFT_ROOM = "LEFT_ROOM"
    const val EVENT_ERROR = "ERROR"
    const val EVENT_EXCEPTION = "EXCEPTION"

    // Business events
    const val EVENT_CONVERSATION_CREATED = "CONVERSATION_CREATED"
}
