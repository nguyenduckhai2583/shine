package com.example.shine.data.remote

import com.example.shine.data.remote.dto.ApiErrorResponse
import com.example.shine.domain.model.AuthException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class ApiErrorParser @Inject constructor(private val json: Json) {

    fun toAuthException(throwable: Throwable): AuthException = when (throwable) {
        is HttpException -> AuthException.Server(
            message = parseMessage(throwable) ?: "Something went wrong (${throwable.code()})",
        )
        is IOException -> AuthException.Network()
        else -> AuthException.Unknown(throwable.message)
    }

    private fun parseMessage(exception: HttpException): String? {
        val body = exception.response()?.errorBody()?.string() ?: return null
        return runCatching { json.decodeFromString<ApiErrorResponse>(body).message }.getOrNull()
    }
}
