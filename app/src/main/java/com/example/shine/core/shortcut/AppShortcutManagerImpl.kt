package com.example.shine.core.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import com.example.shine.BuildConfig
import com.example.shine.MainActivity
import com.example.shine.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppShortcutManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppShortcutManager {

    override fun updateRecentChannelShortcut(channelId: String, channelName: String) {
        val deepLinkUri = "https://${BuildConfig.DEEP_LINK_HOST}/chat/channels/$channelId".toUri()
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = deepLinkUri
            putExtra(AppShortcutManager.EXTRA_SHORTCUT_ACTION, AppShortcutManager.ACTION_OPEN_CHANNEL)
            putExtra(AppShortcutManager.EXTRA_CHANNEL_ID, channelId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val formattedName = if (channelName.startsWith("#")) channelName else "#$channelName"
        val shortcut = ShortcutInfoCompat.Builder(context, AppShortcutManager.SHORTCUT_ID_RECENT_CHANNEL)
            .setShortLabel(formattedName.take(12))
            .setLongLabel(context.getString(R.string.shortcut_recent_channel_long, formattedName))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_channel))
            .setIntent(intent)
            .setRank(0)
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }

    override fun removeRecentChannelShortcut() {
        ShortcutManagerCompat.removeDynamicShortcuts(
            context,
            listOf(AppShortcutManager.SHORTCUT_ID_RECENT_CHANNEL),
        )
    }

    override fun removeAllDynamicShortcuts() {
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)
    }
}
