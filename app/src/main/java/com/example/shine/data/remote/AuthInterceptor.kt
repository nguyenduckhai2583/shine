package com.example.shine.data.remote

import com.example.shine.data.local.TokenProvider
import com.example.shine.data.local.WorkspaceProvider
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val workspaceProvider: WorkspaceProvider,
    private val tokenRefresher: TokenRefresher,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider.current()
        if (token == null || request.header(AUTHORIZATION) != null) {
            return chain.proceed(request)
        }

        val workspaceId = workspaceProvider.current()
        val response = chain.proceed(request.withAuth(token, workspaceId))
        return when (response.code) {
            HTTP_TOKEN_EXPIRED -> {
                val newToken = tokenRefresher.refresh(token) ?: return response
                response.close()
                chain.proceed(request.withAuth(newToken, workspaceId))
            }
            HTTP_UNAUTHORIZED -> {
                tokenRefresher.signOut(token)
                response
            }
            else -> response
        }
    }

    private fun Request.withAuth(token: String, workspaceId: String?): Request {
        val builder = newBuilder().header(AUTHORIZATION, "Bearer $token")
        if (workspaceId != null && header(WORKSPACE_ID) == null) {
            builder.header(WORKSPACE_ID, workspaceId)
        }
        if (workspaceId != null && header(X_WORKSPACE_ID) == null) {
            builder.header(X_WORKSPACE_ID, workspaceId)
        }
        return builder.build()
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val WORKSPACE_ID = "workspace-id"
        const val X_WORKSPACE_ID = "x-workspace-id"
        const val HTTP_TOKEN_EXPIRED = 440
    }
}
