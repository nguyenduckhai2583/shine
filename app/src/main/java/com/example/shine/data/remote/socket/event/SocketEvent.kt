package com.example.shine.data.remote.socket.event

import com.example.shine.data.remote.socket.SocketConstants
import com.example.shine.data.remote.socket.model.ConvSocketMessage
import com.example.shine.data.remote.socket.model.ConversationType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

sealed interface SocketEvent {
    val type: String

    companion object {
        val supportedEventTypes: Set<String> = setOf(
            SocketConstants.EVENT_CONVERSATION_CREATED,
        )

        fun parse(type: String, data: JsonObject): SocketEvent? = when (type) {
            SocketConstants.EVENT_CONVERSATION_CREATED -> ConversationCreatedEvent.from(data)
            else -> null
        }
    }
}

data class ConversationCreatedEvent(
    val conversation: ConvSocketMessage,
) : SocketEvent {
    override val type: String = SocketConstants.EVENT_CONVERSATION_CREATED

    companion object {
        fun from(data: JsonObject): ConversationCreatedEvent? {
            return try {
                val typeStr = data["type"]?.jsonPrimitive?.content ?: return null
                val conversationType = ConversationType.entries.firstOrNull {
                    it.name.equals(typeStr, ignoreCase = true)
                } ?: return null
                val workspaceId = data["workspaceId"]?.jsonPrimitive?.contentOrNull
                val message = ConvSocketMessage(
                    type = conversationType,
                    workspaceId = workspaceId,
                    rawJson = data,
                )
                ConversationCreatedEvent(message)
            } catch (_: Exception) {
                null
            }
        }
    }
}
