package com.example.shine.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(val data: T? = null)

@Serializable
data class ApiErrorResponse(
    val message: String? = null,
    val errorCode: String? = null,
)
