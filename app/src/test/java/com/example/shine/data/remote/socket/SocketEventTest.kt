package com.example.shine.data.remote.socket

import com.example.shine.data.remote.socket.event.ConversationCreatedEvent
import com.example.shine.data.remote.socket.event.SocketEvent
import com.example.shine.data.remote.socket.model.ConversationType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocketEventTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parse_channelConversation_returnsConversationCreatedEvent() {
        val payload = """
            {
                "type": "CHANNEL",
                "workspaceId": "ws-123",
                "id": "conv-456",
                "name": "General",
                "isPrivate": false,
                "isEncrypted": false,
                "isDefault": true,
                "createdAt": 1700000000000
            }
        """.trimIndent()

        val jsonObject = json.parseToJsonElement(payload).jsonObject
        val event = SocketEvent.parse(SocketConstants.EVENT_CONVERSATION_CREATED, jsonObject)

        assertNotNull(event)
        assertTrue(event is ConversationCreatedEvent)
        val created = event as ConversationCreatedEvent
        assertEquals(ConversationType.CHANNEL, created.conversation.type)
        assertEquals("ws-123", created.conversation.workspaceId)
        assertEquals(SocketConstants.EVENT_CONVERSATION_CREATED, created.type)
    }

    @Test
    fun parse_directConversation_returnsConversationCreatedEvent() {
        val payload = """
            {
                "type": "DIRECT",
                "workspaceId": "ws-123",
                "id": "dm-789",
                "name": "Direct Chat",
                "recipientIds": ["user-1", "user-2"]
            }
        """.trimIndent()

        val jsonObject = json.parseToJsonElement(payload).jsonObject
        val event = ConversationCreatedEvent.from(jsonObject)

        assertNotNull(event)
        assertEquals(ConversationType.DIRECT, event?.conversation?.type)
        assertEquals("ws-123", event?.conversation?.workspaceId)
    }

    @Test
    fun parse_groupConversation_returnsConversationCreatedEvent() {
        val payload = """
            {
                "type": "GROUP",
                "workspaceId": "ws-123",
                "id": "group-101",
                "name": "Project Group"
            }
        """.trimIndent()

        val jsonObject = json.parseToJsonElement(payload).jsonObject
        val event = ConversationCreatedEvent.from(jsonObject)

        assertNotNull(event)
        assertEquals(ConversationType.GROUP, event?.conversation?.type)
    }

    @Test
    fun parse_threadConversation_returnsConversationCreatedEvent() {
        val payload = """
            {
                "type": "THREAD",
                "workspaceId": "ws-123",
                "id": "thread-202"
            }
        """.trimIndent()

        val jsonObject = json.parseToJsonElement(payload).jsonObject
        val event = ConversationCreatedEvent.from(jsonObject)

        assertNotNull(event)
        assertEquals(ConversationType.THREAD, event?.conversation?.type)
    }

    @Test
    fun parse_missingType_returnsNull() {
        val payload = """
            {
                "workspaceId": "ws-123",
                "id": "conv-456"
            }
        """.trimIndent()

        val jsonObject = json.parseToJsonElement(payload).jsonObject
        val event = ConversationCreatedEvent.from(jsonObject)

        assertNull(event)
    }

    @Test
    fun parse_invalidType_returnsNull() {
        val payload = """
            {
                "type": "UNKNOWN_TYPE",
                "workspaceId": "ws-123"
            }
        """.trimIndent()

        val jsonObject = json.parseToJsonElement(payload).jsonObject
        val event = ConversationCreatedEvent.from(jsonObject)

        assertNull(event)
    }
}
