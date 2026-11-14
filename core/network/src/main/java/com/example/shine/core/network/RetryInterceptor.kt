package com.example.shine.core.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors employer-mobile `RetryInterceptor`:
 * Retries requests when receiving transient server errors (500, 502, 503, 504).
 * HTTP 429 (Too Many Requests) is explicitly NOT retried.
 * Retries up to [maxRetries] times with configurable delays (default: 1s, 3s, 5s).
 */
@Singleton
class RetryInterceptor(
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
    private val retryDelays: List<Long> = DEFAULT_RETRY_DELAYS_MS,
    private val sleeper: (Long) -> Unit = { delayMs ->
        try {
            Thread.sleep(delayMs)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    },
) : Interceptor {

    @Inject
    constructor() : this(DEFAULT_MAX_RETRIES, DEFAULT_RETRY_DELAYS_MS)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var attempt = 0

        while (!response.isSuccessful && shouldRetry(response, request) && attempt < maxRetries) {
            val delayMs = retryDelays.getOrElse(attempt) { retryDelays.last() }

            Log.d(
                TAG,
                "HTTP ${response.code} on ${request.url}. Retrying attempt ${attempt + 1}/$maxRetries after ${delayMs}ms",
            )

            response.close()
            attempt++

            if (delayMs > 0) {
                sleeper(delayMs)
            }

            response = chain.proceed(request)
        }

        return response
    }

    private fun shouldRetry(response: Response, request: Request): Boolean {
        if (request.body?.isOneShot() == true) {
            return false
        }
        if (response.code == HTTP_TOO_MANY_REQUESTS) {
            return false
        }
        return response.code in RETRYABLE_STATUS_CODES
    }

    companion object {
        private const val TAG = "RetryInterceptor"
        const val DEFAULT_MAX_RETRIES = 3
        val DEFAULT_RETRY_DELAYS_MS = listOf(1000L, 3000L, 5000L)

        const val HTTP_TOO_MANY_REQUESTS = 429
        val RETRYABLE_STATUS_CODES = setOf(
            500, // Internal Server Error
            502, // Bad Gateway
            503, // Service Unavailable
            504, // Gateway Timeout
        )
    }
}
