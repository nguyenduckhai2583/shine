package com.example.shine.domain.model

enum class AppError {
    NO_INTERNET,
    SERVER,
    TWO_FACTOR_REQUIRED,
    UNKNOWN,
}

class AppException(
    val error: AppError,
    val serverMessage: String? = null,
    cause: Throwable? = null,
) : Exception(serverMessage ?: error.name, cause)
