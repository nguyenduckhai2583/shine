package com.example.shine.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponse(
    val message: String? = null,
    val errorCode: String? = null,
)
