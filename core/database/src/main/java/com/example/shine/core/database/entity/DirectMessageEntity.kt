package com.example.shine.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.shine.core.model.DirectMessage

@Entity(tableName = "direct_messages")
data class DirectMessageEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val time: Long?,
    val type: String? = null,
    val createdAt: Long? = null,
    val lastActivityAt: Long? = null,
)

fun DirectMessageEntity.toDomain(): DirectMessage = DirectMessage(
    id = id,
    name = name,
    time = time,
    type = type,
    createdAt = createdAt,
    lastActivityAt = lastActivityAt,
)

fun DirectMessage.toEntity(): DirectMessageEntity = DirectMessageEntity(
    id = id,
    name = name,
    time = time,
    type = type,
    createdAt = createdAt,
    lastActivityAt = lastActivityAt,
)
