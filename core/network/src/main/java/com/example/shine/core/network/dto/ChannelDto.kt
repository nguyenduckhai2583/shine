package com.example.shine.core.network.dto

import com.example.shine.core.model.Channel
import kotlinx.serialization.Serializable

@Serializable
data class ChannelDto(
    val id: String,
    val name: String? = null,
    val isPrivate: Boolean? = null,
    val isEncrypted: Boolean? = null,
    val isDefault: Boolean? = null,
    val createdAt: Long? = null,
    val lastActivityAt: Long? = null,
    val position: Long? = null,
    val category: CategoryDto? = null,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String? = null,
)

fun ChannelDto.toDomain() = Channel(
    id = id,
    name = name.orEmpty(),
    isPrivate = isPrivate == true,
    isEncrypted = isEncrypted == true,
    isDefault = isDefault == true,
    categoryName = category?.name,
    createdAt = createdAt,
    lastActivityAt = lastActivityAt,
)

fun ChannelDto.toEntity() = com.example.shine.core.database.entity.ChannelEntity(
    id = id,
    localId = id,
    name = name.orEmpty(),
    isPrivate = isPrivate == true,
    isEncrypted = isEncrypted == true,
    isDefault = isDefault == true,
    categoryName = category?.name,
    createdAt = createdAt ?: System.currentTimeMillis(),
    lastActivityAt = lastActivityAt ?: System.currentTimeMillis(),
    position = position,
    syncStatus = com.example.shine.core.model.SyncStatus.SYNCED,
)
