package com.example.shine.core.shortcut

interface AppShortcutManager {
    fun updateRecentChannelShortcut(channelId: String, channelName: String)
    fun removeRecentChannelShortcut()
    fun removeAllDynamicShortcuts()

    companion object {
        const val SHORTCUT_ID_RECENT_CHANNEL = "recent_channel"
        const val EXTRA_SHORTCUT_ACTION = "shortcut_action"
        const val EXTRA_CHANNEL_ID = "channel_id"
        const val ACTION_OPEN_PROFILE = "open_profile"
        const val ACTION_OPEN_CHANNEL = "open_channel"
    }
}
