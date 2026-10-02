package com.example.shine.data.remote.dto

import com.example.shine.domain.model.Channel
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
