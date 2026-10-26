package com.example.shine.data.remote.dto

import com.example.shine.domain.model.DirectMessage
import kotlinx.serialization.Serializable

@Serializable
data class DirectMessageDto(
    val id: String,
    val name: String? = null,
    val type: String? = null,
    val recipientIds: List<String>? = null,
    val lastActivityAt: Long? = null,
    val latestMessageAt: Long? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val members: List<DirectMessageMemberDto>? = null,
    val latestMessage: DirectMessageLatestMessageDto? = null,
)

@Serializable
data class DirectMessageMemberDto(
    val id: String,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
)

@Serializable
data class DirectMessageLatestMessageDto(
    val id: String? = null,
    val createdAt: Long? = null,
)

fun DirectMessageDto.toDomain(
    currentUserId: String? = null,
    chatUsersMap: Map<String, String> = emptyMap(),
): DirectMessage {
    val resolvedName = when {
        !name.isNullOrBlank() -> name
        !members.isNullOrEmpty() -> {
            val otherMembers = members.filter { it.id != currentUserId }
            val membersToUse = if (otherMembers.isNotEmpty()) otherMembers else members
            val names = membersToUse.mapNotNull { member ->
                val full = listOfNotNull(member.firstName, member.lastName).joinToString(" ").trim()
                full.ifEmpty { member.email }
            }
            if (names.isNotEmpty()) names.joinToString(", ") else "Direct Message"
        }
        !recipientIds.isNullOrEmpty() -> {
            val otherIds = recipientIds.filter { it != currentUserId }
            val idsToUse = if (otherIds.isNotEmpty()) otherIds else recipientIds
            val names = idsToUse.mapNotNull { chatUsersMap[it]?.takeIf { name -> name.isNotBlank() } }
            if (names.isNotEmpty()) names.joinToString(", ") else "Direct Message"
        }
        else -> "Direct Message"
    }

    val resolvedTime = latestMessageAt
        ?: latestMessage?.createdAt
        ?: lastActivityAt
        ?: updatedAt
        ?: createdAt

    return DirectMessage(
        id = id,
        name = resolvedName,
        time = resolvedTime,
        type = type,
    )
}
