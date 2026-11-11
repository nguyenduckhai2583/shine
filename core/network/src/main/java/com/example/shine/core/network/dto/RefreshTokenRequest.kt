package com.example.shine.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenRequest(val refreshToken: String)
