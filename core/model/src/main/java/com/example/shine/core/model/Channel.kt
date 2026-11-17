package com.example.shine.core.model

data class Channel(
    override val id: String,
    override val name: String,
    val isPrivate: Boolean,
    val isEncrypted: Boolean,
    val isDefault: Boolean,
    val categoryName: String?,
    override val createdAt: Long?,
    override val lastActivityAt: Long?,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val syncErrorMessage: String? = null,
) : Conversation
