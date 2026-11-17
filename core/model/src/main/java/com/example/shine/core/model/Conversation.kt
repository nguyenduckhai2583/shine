package com.example.shine.core.model

sealed interface Conversation {
    val id: String
    val name: String
    val createdAt: Long?
    val lastActivityAt: Long?
}
