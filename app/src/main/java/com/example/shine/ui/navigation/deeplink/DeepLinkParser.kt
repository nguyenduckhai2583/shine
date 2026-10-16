package com.example.shine.ui.navigation.deeplink

import android.net.Uri
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepLinkParser @Inject constructor() {

    fun parse(uri: Uri?): DeepLinkDestination? = parse(uri?.toString())

    fun parse(url: String?): DeepLinkDestination? {
        if (url.isNullOrBlank()) return null

        val parsedUri = try {
            URI.create(url)
        } catch (_: Exception) {
            return null
        }

        val scheme = parsedUri.scheme ?: return null
        if (!scheme.equals(DeepLinkConstants.SCHEME_HTTPS, ignoreCase = true)) {
            return null
        }

        val host = parsedUri.host?.lowercase() ?: return null
        val baseHost = DeepLinkConstants.HOST_BASE.lowercase()

        val workspaceSubdomain = when {
            host == baseHost -> null
            host.endsWith(".$baseHost") -> host.removeSuffix(".$baseHost")
            else -> return null
        }

        val path = parsedUri.rawPath ?: return null
        val matchResult = DeepLinkConstants.CONVERSATION_REGEX.find(path) ?: return null

        val type = matchResult.groupValues[1]
        val conversationId = matchResult.groupValues[2]
        val messageId = matchResult.groupValues.getOrNull(3)?.ifEmpty { null }

        return when (type.lowercase()) {
            DeepLinkConstants.PATH_CHANNELS -> DeepLinkDestination.Channel(
                channelId = conversationId,
                messageId = messageId,
                workspaceSubdomain = workspaceSubdomain,
            )
            else -> null
        }
    }
}
