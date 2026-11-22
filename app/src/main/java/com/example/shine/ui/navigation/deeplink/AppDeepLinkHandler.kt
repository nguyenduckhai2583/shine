package com.example.shine.ui.navigation.deeplink

import android.content.Intent
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.BackStackMatcher
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.deeplink.invoke
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDeepLinkHandler @Inject constructor(
    private val matchers: Set<@JvmSuppressWildcards BackStackMatcher<*, NavKey>>,
) {

    /**
     * Matches an incoming [Intent] against all registered [BackStackMatcher]s.
     * Handles both Web URL Deep Links (via intent.data) and App Shortcuts (via intent.extras).
     */
    fun handleIntent(intent: Intent?): List<NavKey>? {
        if (intent == null) return null
        val request = try {
            DeepLinkRequest(intent)
        } catch (_: Exception) {
            null
        } ?: return null

        return matchers.firstNotNullOfOrNull { it.match(request) }?.backStack
    }
}
