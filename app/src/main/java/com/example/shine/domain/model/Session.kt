package com.example.shine.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val token: String,
    val refreshToken: String? = null,
    val expireAt: Long? = null,
    val user: User? = null,
    val workspaceId: String? = null,
)

@Serializable
data class User(
    val id: String,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val avatarUrl: String? = null,
) {
    val displayName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { email.orEmpty() }

    val avatarPath: String?
        get() = avatarUrl
}

