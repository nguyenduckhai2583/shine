package com.example.shine.core.network.socket.model

import kotlinx.serialization.json.JsonObject

data class ConvSocketMessage(
    val type: ConversationType,
    val workspaceId: String? = null,
    val rawJson: JsonObject,
)
