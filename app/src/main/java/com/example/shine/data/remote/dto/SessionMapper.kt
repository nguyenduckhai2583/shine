package com.example.shine.data.remote.dto

import com.example.shine.domain.model.Session
import com.example.shine.domain.model.User

fun SessionDto.toDomain() = Session(
    token = token,
    refreshToken = refreshToken,
    expireAt = expireAt,
    user = user?.toDomain(),
)

fun UserDto.toDomain() = User(id = id, email = email, firstName = firstName, lastName = lastName)
