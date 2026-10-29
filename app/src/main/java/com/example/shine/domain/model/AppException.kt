package com.example.shine.domain.model

enum class AppErrorEnum {
    NO_INTERNET,
    SERVER,
    UNKNOWN,
}

class AppException(
    val type: AppErrorEnum,
    val errorMessage: String? = null,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : Exception(errorMessage ?: type.name, cause) {

    constructor(type: AppErrorEnum, cause: Throwable?) : this(
        type = type,
        errorMessage = null,
        statusCode = null,
        cause = cause,
    )

    constructor(type: AppErrorEnum, errorMessage: String?, cause: Throwable? = null) : this(
        type = type,
        errorMessage = errorMessage,
        statusCode = null,
        cause = cause,
    )
}
