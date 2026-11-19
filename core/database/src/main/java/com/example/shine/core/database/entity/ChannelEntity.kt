package com.example.shine.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.shine.core.model.Channel
import com.example.shine.core.model.SyncStatus
import java.util.UUID

@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["workspaceId", "id"]),
    ],
)
data class ChannelEntity(
    @PrimaryKey
    val localId: String = UUID.randomUUID().toString(),
    val id: String? = null,
    val workspaceId: String,
    val name: String,
    val isPrivate: Boolean = false,
    val isEncrypted: Boolean = false,
    val isDefault: Boolean = false,
    val categoryName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivityAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val syncErrorMessage: String? = null,
)

fun ChannelEntity.toDomain(): Channel = Channel(
    id = id ?: localId,
    name = name,
    isPrivate = isPrivate,
    isEncrypted = isEncrypted,
    isDefault = isDefault,
    categoryName = categoryName,
    createdAt = createdAt,
    lastActivityAt = lastActivityAt,
    syncStatus = syncStatus,
    syncErrorMessage = syncErrorMessage,
)

fun Channel.toEntity(workspaceId: String): ChannelEntity = ChannelEntity(
    localId = id,
    id = if (syncStatus == SyncStatus.SYNCED) id else null,
    workspaceId = workspaceId,
    name = name,
    isPrivate = isPrivate,
    isEncrypted = isEncrypted,
    isDefault = isDefault,
    categoryName = categoryName,
    createdAt = createdAt ?: System.currentTimeMillis(),
    lastActivityAt = lastActivityAt ?: System.currentTimeMillis(),
    syncStatus = syncStatus,
    syncErrorMessage = syncErrorMessage,
)
