package com.example.shine.ui.navigation.deeplink

import com.example.shine.BuildConfig

object DeepLinkConstants {
    const val SCHEME_HTTPS = "https"
    const val HOST_BASE = BuildConfig.DEEP_LINK_HOST

    const val PATH_CHANNELS = "channels"
    const val PATH_DIRECT_MESSAGES = "direct-messages"

    val CONVERSATION_REGEX = Regex(
        pattern = """^/chat/($PATH_CHANNELS|$PATH_DIRECT_MESSAGES)/([a-zA-Z0-9_-]+)(?:/([a-zA-Z0-9_-]+))?/?$""",
        option = RegexOption.IGNORE_CASE,
    )
}

sealed interface DeepLinkDestination {
    data class Channel(
        val channelId: String,
        val messageId: String? = null,
        val workspaceSubdomain: String? = null,
    ) : DeepLinkDestination
}
