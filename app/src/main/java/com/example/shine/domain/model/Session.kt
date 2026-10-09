package com.example.shine.domain.model

data class Session(
    val token: String,
    val refreshToken: String?,
    val expireAt: Long?,
    val user: User?,
    val workspaceId: String? = null,
)

data class User(
    val id: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
) {
    val displayName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { email.orEmpty() }
}
