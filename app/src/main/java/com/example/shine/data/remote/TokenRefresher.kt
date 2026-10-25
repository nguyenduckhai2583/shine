package com.example.shine.data.remote

import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.remote.dto.RefreshTokenRequest
import com.example.shine.data.remote.dto.toDomain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors employer-mobile `TokenRefresher`: one refresh at a time, a 401 from the
 * refresh endpoint signs the user out, any other failure keeps the session.
 */
@Singleton
class TokenRefresher @Inject constructor(
    private val api: TokenRefreshApi,
    private val local: SessionLocalDataSource,
) {
    private val lock = Any()

    /** Returns a token newer than [expiredToken], or null if refreshing failed. */
    fun refresh(expiredToken: String): String? = synchronized(lock) {
        val session = runBlocking { local.session.first() } ?: return null
        // Another request already refreshed while this one was waiting for the lock.
        if (session.token != expiredToken) return session.token

        val refreshToken = session.refreshToken ?: return signOut(expiredToken)

        val response = try {
            api.refreshToken(RefreshTokenRequest(refreshToken)).execute()
        } catch (_: Exception) {
            return null
        }
        if (response.code() == HTTP_UNAUTHORIZED) return signOut(expiredToken)

        val fresh = response.body()?.toDomain() ?: return null


        val merged = fresh.copy(
            refreshToken = fresh.refreshToken ?: session.refreshToken,
            expireAt = fresh.expireAt ?: session.expireAt,
            user = fresh.user ?: session.user,
            workspaceId = fresh.workspaceId ?: session.workspaceId,
        )
        runBlocking { local.save(merged) }
        merged.token
    }

    /** Signs out only if [invalidToken] is still the current one, so a newer login survives. */
    fun signOut(invalidToken: String): String? = synchronized(lock) {
        runBlocking {
            if (local.session.first()?.token == invalidToken) local.clear()
        }
        null
    }
}
