package com.example.shine.ui.navigation.deeplink

import android.os.Bundle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.DeepLinkMatcher
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.deeplink.IntentExtrasKey
import com.example.shine.core.shortcut.AppShortcutManager
import com.example.shine.ui.navigation.ChannelDetail
import com.example.shine.ui.navigation.Profile

/**
 * [DeepLinkMatcher] for matching Android App Shortcuts passed as Intent extras.
 */
class ShortcutDeepLinkMatcher : DeepLinkMatcher<NavKey, DeepLinkMatcher.MatchResult<NavKey>>() {

    override fun matchRequest(request: DeepLinkRequest): MatchResult<NavKey>? {
        val bundle: Bundle = request.extras[DeepLinkRequest.IntentExtrasKey] ?: return null
        val shortcutAction = bundle.getString(AppShortcutManager.EXTRA_SHORTCUT_ACTION)
        val channelId = bundle.getString(AppShortcutManager.EXTRA_CHANNEL_ID)

        val navKey: NavKey = when {
            shortcutAction == AppShortcutManager.ACTION_OPEN_PROFILE -> Profile
            !channelId.isNullOrBlank() -> ChannelDetail(channelId = channelId)
            else -> return null
        }
        return MatchResult(navKey)
    }
}
