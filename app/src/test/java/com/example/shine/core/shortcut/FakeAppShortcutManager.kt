package com.example.shine.core.shortcut

class FakeAppShortcutManager : AppShortcutManager {
    var lastUpdatedChannelId: String? = null
    var lastUpdatedChannelName: String? = null
    var removeRecentCalls = 0
    var removeAllCalls = 0

    override fun updateRecentChannelShortcut(channelId: String, channelName: String) {
        lastUpdatedChannelId = channelId
        lastUpdatedChannelName = channelName
    }

    override fun removeRecentChannelShortcut() {
        removeRecentCalls++
        lastUpdatedChannelId = null
        lastUpdatedChannelName = null
    }

    override fun removeAllDynamicShortcuts() {
        removeAllCalls++
        lastUpdatedChannelId = null
        lastUpdatedChannelName = null
    }
}
