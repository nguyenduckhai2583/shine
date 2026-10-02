package com.example.shine.data.remote

import com.example.shine.data.remote.dto.ApiErrorResponse
import com.example.shine.domain.model.AuthException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

private val errorJson = Json { ignoreUnknownKeys = true }

/** Runs an API [block] and maps failures to [AuthException], rethrowing cancellation. */
suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e.toAuthException())
}

private fun Throwable.toAuthException(): AuthException = when (this) {
    is AuthException -> this
    is HttpException -> AuthException.Server(
        message = parseMessage() ?: "Something went wrong (${code()})",
    )
    is IOException -> AuthException.Network()
    else -> AuthException.Unknown(message)
}

private fun HttpException.parseMessage(): String? {
    val body = response()?.errorBody()?.string() ?: return null
    return runCatching { errorJson.decodeFromString<ApiErrorResponse>(body).message }.getOrNull()
}
