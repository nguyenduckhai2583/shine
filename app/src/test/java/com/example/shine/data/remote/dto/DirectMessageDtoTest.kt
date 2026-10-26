package com.example.shine.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Test

class DirectMessageDtoTest {

    @Test
    fun toDomain_withCustomName_usesCustomName() {
        val dto = DirectMessageDto(
            id = "dm-1",
            name = "Project Discussion",
            lastActivityAt = 1000L,
        )

        val domain = dto.toDomain(currentUserId = "user-me")
        assertEquals("dm-1", domain.id)
        assertEquals("Project Discussion", domain.name)
        assertEquals(1000L, domain.time)
    }

    @Test
    fun toDomain_withoutName_usesOtherMemberName() {
        val dto = DirectMessageDto(
            id = "dm-2",
            name = null,
            members = listOf(
                DirectMessageMemberDto(id = "user-me", firstName = "My", lastName = "Self"),
                DirectMessageMemberDto(id = "user-other", firstName = "Jane", lastName = "Doe"),
            ),
            lastActivityAt = 2000L,
        )

        val domain = dto.toDomain(currentUserId = "user-me")
        assertEquals("Jane Doe", domain.name)
        assertEquals(2000L, domain.time)
    }

    @Test
    fun toDomain_withoutNameOrMembers_resolvesFromChatUsersMap() {
        val dto = DirectMessageDto(
            id = "dm-3",
            name = null,
            recipientIds = listOf("user-me", "user-other"),
            createdAt = 3000L,
        )

        val chatUsers = mapOf("user-other" to "Bob Smith")
        val domain = dto.toDomain(currentUserId = "user-me", chatUsersMap = chatUsers)
        assertEquals("Bob Smith", domain.name)
        assertEquals(3000L, domain.time)
    }

    @Test
    fun toDomain_prefersLatestMessageAtOverLastActivityAt() {
        val dto = DirectMessageDto(
            id = "dm-4",
            name = "Test",
            latestMessageAt = 5000L,
            lastActivityAt = 4000L,
            createdAt = 1000L,
        )

        val domain = dto.toDomain()
        assertEquals(5000L, domain.time)
    }
}
