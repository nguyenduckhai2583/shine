package com.example.shine.core.data.session

import com.example.shine.core.data.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preloads essential user data (user profile and workspace chat users)
 * once when entering an authenticated session to prime local caches.
 */
@Singleton
class SessionPreloader @Inject constructor(
    private val userRepository: UserRepository,
) {
    private var hasPreloaded = false

    suspend fun preloadIfNeeded() {
        if (!hasPreloaded) {
            supervisorScope {
                awaitAll(
                    async { runCatching { userRepository.getProfile() } },
                    async { runCatching { userRepository.getChatUsers() } },
                )
            }
            hasPreloaded = true
        }
    }

    fun reset() {
        hasPreloaded = false
    }
}
