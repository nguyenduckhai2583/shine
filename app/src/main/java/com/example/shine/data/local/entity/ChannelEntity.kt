package com.example.shine.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.shine.data.remote.dto.ChannelDto
import com.example.shine.domain.model.Channel

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isPrivate: Boolean,
    val isEncrypted: Boolean,
    val isDefault: Boolean,
    val categoryName: String?,
    val createdAt: Long?,
    val lastActivityAt: Long?,
    val position: Long?,
)

fun ChannelEntity.toDomain() = Channel(
    id = id,
    name = name,
    isPrivate = isPrivate,
    isEncrypted = isEncrypted,
    isDefault = isDefault,
    categoryName = categoryName,
    createdAt = createdAt,
    lastActivityAt = lastActivityAt,
)

fun ChannelDto.toEntity() = ChannelEntity(
    id = id,
    name = name.orEmpty(),
    isPrivate = isPrivate == true,
    isEncrypted = isEncrypted == true,
    isDefault = isDefault == true,
    categoryName = category?.name,
    createdAt = createdAt,
    lastActivityAt = lastActivityAt,
    position = position,
)

fun Channel.toEntity(position: Long? = null) = ChannelEntity(
    id = id,
    name = name,
    isPrivate = isPrivate,
    isEncrypted = isEncrypted,
    isDefault = isDefault,
    categoryName = categoryName,
    createdAt = createdAt,
    lastActivityAt = lastActivityAt,
    position = position,
)
