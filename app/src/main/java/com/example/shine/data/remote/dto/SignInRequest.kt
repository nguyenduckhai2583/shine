package com.example.shine.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SignInRequest(
    val email: String,
    val password: String,
)
