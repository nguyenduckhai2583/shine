package com.example.shine.domain.model

data class Channel(
    val id: String,
    val name: String,
    val isPrivate: Boolean,
    val isEncrypted: Boolean,
    val isDefault: Boolean,
    val categoryName: String?,
    val createdAt: Long?,
    val lastActivityAt: Long?,
)
