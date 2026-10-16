package com.example.shine.ui.navigation.deeplink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeepLinkParserTest {

    private lateinit var parser: DeepLinkParser

    @Before
    fun setUp() {
        parser = DeepLinkParser()
    }

    @Test
    fun parse_validChannelDeepLinkOnBareHost_returnsChannelDestination() {
        val url = "https://${DeepLinkConstants.HOST_BASE}/chat/channels/general-123"
        val destination = parser.parse(url)

        assertNotNull(destination)
        assertTrue(destination is DeepLinkDestination.Channel)
        val channel = destination as DeepLinkDestination.Channel
        assertEquals("general-123", channel.channelId)
        assertNull(channel.messageId)
        assertNull(channel.workspaceSubdomain)
    }

    @Test
    fun parse_validChannelDeepLinkWithSubdomain_returnsChannelWithWorkspaceSubdomain() {
        val url = "https://diginex.${DeepLinkConstants.HOST_BASE}/chat/channels/team-dev"
        val destination = parser.parse(url)

        assertNotNull(destination)
        assertTrue(destination is DeepLinkDestination.Channel)
        val channel = destination as DeepLinkDestination.Channel
        assertEquals("team-dev", channel.channelId)
        assertNull(channel.messageId)
        assertEquals("diginex", channel.workspaceSubdomain)
    }

    @Test
    fun parse_validChannelDeepLinkWithMessageId_returnsChannelAndMessageId() {
        val url = "https://diginex.${DeepLinkConstants.HOST_BASE}/chat/channels/team-dev/msg-999"
        val destination = parser.parse(url)

        assertNotNull(destination)
        assertTrue(destination is DeepLinkDestination.Channel)
        val channel = destination as DeepLinkDestination.Channel
        assertEquals("team-dev", channel.channelId)
        assertEquals("msg-999", channel.messageId)
        assertEquals("diginex", channel.workspaceSubdomain)
    }

    @Test
    fun parse_customSchemeOrHttp_returnsNull() {
        assertNull(parser.parse("shine://channel/123"))
        assertNull(parser.parse("http://${DeepLinkConstants.HOST_BASE}/chat/channels/123"))
    }

    @Test
    fun parse_foreignHost_returnsNull() {
        assertNull(parser.parse("https://random-site.com/chat/channels/123"))
    }

    @Test
    fun parse_unsupportedPath_returnsNull() {
        assertNull(parser.parse("https://${DeepLinkConstants.HOST_BASE}/chat/random/123"))
        assertNull(parser.parse("https://${DeepLinkConstants.HOST_BASE}/unknown"))
    }

    @Test
    fun parse_emptyOrNull_returnsNull() {
        assertNull(parser.parse(null as String?))
        assertNull(parser.parse(""))
        assertNull(parser.parse("   "))
    }
}
