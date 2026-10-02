package com.example.shine.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponse(
    val message: String? = null,
    val errorCode: String? = null,
)
