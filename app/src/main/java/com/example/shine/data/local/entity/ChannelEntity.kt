package com.example.shine.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.shine.domain.model.Channel
import com.example.shine.domain.model.SyncStatus
import java.util.UUID

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey
    val localId: String = UUID.randomUUID().toString(),
    val id: String? = null,
    val name: String,
    val isPrivate: Boolean = false,
    val isEncrypted: Boolean = false,
    val isDefault: Boolean = false,
    val categoryName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivityAt: Long = System.currentTimeMillis(),
    val position: Long? = null,
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

fun Channel.toEntity(position: Long? = null): ChannelEntity = ChannelEntity(
    localId = id,
    id = if (syncStatus == SyncStatus.SYNCED) id else null,
    name = name,
    isPrivate = isPrivate,
    isEncrypted = isEncrypted,
    isDefault = isDefault,
    categoryName = categoryName,
    createdAt = createdAt ?: System.currentTimeMillis(),
    lastActivityAt = lastActivityAt ?: System.currentTimeMillis(),
    position = position,
    syncStatus = syncStatus,
    syncErrorMessage = syncErrorMessage,
)
