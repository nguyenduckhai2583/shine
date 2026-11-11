package com.example.shine.core.network.socket.model

import kotlinx.serialization.Serializable

@Serializable
enum class ConversationType {
    CHANNEL,
    THREAD,
    DIRECT,
    GROUP,
}
