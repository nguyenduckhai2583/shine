package com.example.shine.data.remote.socket.model

import kotlinx.serialization.Serializable

@Serializable
enum class ConversationType {
    CHANNEL,
    THREAD,
    DIRECT,
    GROUP,
}
