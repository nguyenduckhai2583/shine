package com.example.shine.data.remote.socket.model

import kotlinx.serialization.json.JsonObject

data class ConvSocketMessage(
    val type: ConversationType,
    val workspaceId: String? = null,
    val rawJson: JsonObject,
)
