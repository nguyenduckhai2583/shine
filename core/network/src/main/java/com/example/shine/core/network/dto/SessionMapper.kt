package com.example.shine.core.network.dto

import com.example.shine.core.model.Session
import com.example.shine.core.model.User

fun SessionDto.toDomain() = Session(
    token = token,
    refreshToken = refreshToken,
    expireAt = expireAt,
    user = user?.toDomain(),
)

fun UserDto.toDomain() = User(
    id = id,
    email = email,
    firstName = firstName,
    lastName = lastName,
    avatarUrl = avatarUrl?.takeIf { it.isNotBlank() } ?: avatar?.signedUrl,
)

