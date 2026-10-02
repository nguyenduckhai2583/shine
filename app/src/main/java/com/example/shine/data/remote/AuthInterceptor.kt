package com.example.shine.data.remote

import com.example.shine.data.local.TokenProvider
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val tokenRefresher: TokenRefresher,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider.current()
        if (token == null || request.header(AUTHORIZATION) != null) {
            return chain.proceed(request)
        }

        val response = chain.proceed(request.withToken(token))
        return when (response.code) {
            HTTP_TOKEN_EXPIRED -> {
                val newToken = tokenRefresher.refresh(token) ?: return response
                response.close()
                chain.proceed(request.withToken(newToken))
            }
            HTTP_UNAUTHORIZED -> {
                tokenRefresher.signOut(token)
                response
            }
            else -> response
        }
    }

    private fun Request.withToken(token: String) =
        newBuilder().header(AUTHORIZATION, "Bearer $token").build()

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val HTTP_TOKEN_EXPIRED = 440
    }
}
