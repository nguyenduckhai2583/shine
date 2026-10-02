package com.example.shine.domain.model

data class Channel(
    val id: String,
    val name: String,
    val isPrivate: Boolean,
    val isEncrypted: Boolean,
    val isDefault: Boolean,
    val categoryName: String?,
    /** Epoch seconds. */
    val createdAt: Long?,
    /** Epoch seconds. */
    val lastActivityAt: Long?,
)
