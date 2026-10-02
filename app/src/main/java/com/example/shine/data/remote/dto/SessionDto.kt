package com.example.shine.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SessionDto(
    val token: String,
    val refreshToken: String? = null,
    val expireAt: Long? = null,
    val isTmpToken: Boolean? = null,
    val user: UserDto? = null,
)

@Serializable
data class UserDto(
    val id: String,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val avatarUrl: String? = null,
)
