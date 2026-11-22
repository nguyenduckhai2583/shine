package com.example.shine.ui.navigation.deeplink

import androidx.navigation3.runtime.deeplink.DeepLinkUri
import androidx.navigation3.runtime.deeplink.UriDeepLinkMatcher
import androidx.navigation3.runtime.deeplink.UriMatchResult
import kotlinx.serialization.KSerializer

/**
 * Custom [UriDeepLinkMatcher] that supports wildcard subdomains (e.g., `*.DEEP_LINK_HOST`)
 * and trailing slash normalization for the Shine application.
 */
open class AppDeepLinkMatcher<out T : Any>(
    uriPattern: DeepLinkUri,
    serializer: KSerializer<T>,
) : UriDeepLinkMatcher<T>(uriPattern, serializer) {

    override fun matchUri(uri: DeepLinkUri): UriMatchResult<T>? {
        val host = uri.host?.lowercase() ?: return null
        val baseHost = DeepLinkConstants.HOST_BASE.lowercase()
        val normalizedHost = if ((host != baseHost) && host.endsWith(".$baseHost")) {
            baseHost
        } else {
            host
        }

        val path = uri.path
        val normalizedPath = if (path != null && path.length > 1 && path.endsWith("/")) {
            path.dropLast(1)
        } else {
            path
        }

        val normalizedUri = uri.buildUpon()
            .authority(normalizedHost)
            .path(normalizedPath)
            .build()

        return super.matchUri(normalizedUri)
    }
}
