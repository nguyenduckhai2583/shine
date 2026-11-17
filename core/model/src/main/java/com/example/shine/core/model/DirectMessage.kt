package com.example.shine.core.model

data class DirectMessage(
    override val id: String,
    override val name: String,
    val time: Long?,
    val type: String? = null,
    override val createdAt: Long? = null,
    override val lastActivityAt: Long? = null,
) : Conversation
