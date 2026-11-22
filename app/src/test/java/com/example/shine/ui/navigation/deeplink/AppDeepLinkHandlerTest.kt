package com.example.shine.ui.navigation.deeplink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.example.shine.core.shortcut.AppShortcutManager
import com.example.shine.ui.navigation.ChannelDetail
import com.example.shine.ui.navigation.DirectMessageDetail
import com.example.shine.ui.navigation.Home
import com.example.shine.ui.navigation.Profile
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`

class AppDeepLinkHandlerTest {

    companion object {
        private lateinit var mockedUri: MockedStatic<Uri>

        @BeforeClass
        @JvmStatic
        fun beforeAll() {
            mockedUri = mockStatic(Uri::class.java)
            mockedUri.`when`<Uri> { Uri.parse(anyString()) }.thenAnswer { invocation ->
                val str = invocation.getArgument<String>(0)
                createMockUri(str)
            }
            mockedUri.`when`<String> { Uri.decode(anyString()) }.thenAnswer { invocation ->
                invocation.getArgument<String>(0)
            }
            mockedUri.`when`<String> { Uri.encode(anyString()) }.thenAnswer { invocation ->
                invocation.getArgument<String>(0)
            }
            mockedUri.`when`<String> { Uri.encode(anyString(), org.mockito.ArgumentMatchers.nullable(String::class.java)) }.thenAnswer { invocation ->
                invocation.getArgument<String>(0)
            }
        }

        @AfterClass
        @JvmStatic
        fun afterAll() {
            mockedUri.close()
        }

        private fun createMockUri(url: String): Uri {
            val uriMock = mock(Uri::class.java)
            `when`(uriMock.toString()).thenReturn(url)

            val uriRegex = Regex("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?")
            val match = uriRegex.matchEntire(url)
            val scheme = match?.groups?.get(2)?.value
            val authority = match?.groups?.get(4)?.value
            val host = authority?.substringBefore(':')
            val path = match?.groups?.get(5)?.value
            val query = match?.groups?.get(7)?.value
            val fragment = match?.groups?.get(9)?.value
            val segments = path?.split('/')?.filter { it.isNotEmpty() } ?: emptyList()

            `when`(uriMock.scheme).thenReturn(scheme)
            `when`(uriMock.authority).thenReturn(authority)
            `when`(uriMock.host).thenReturn(host)
            `when`(uriMock.path).thenReturn(path)
            `when`(uriMock.pathSegments).thenReturn(segments)
            `when`(uriMock.query).thenReturn(query)
            `when`(uriMock.fragment).thenReturn(fragment)
            `when`(uriMock.isHierarchical).thenReturn(scheme != null)

            val queryParamNames = query?.split('&')
                ?.mapNotNull { it.substringBefore('=').takeIf { name -> name.isNotEmpty() } }
                ?.toSet() ?: emptySet()
            `when`(uriMock.queryParameterNames).thenReturn(queryParamNames)

            val builderMock = mock(Uri.Builder::class.java)
            var currentAuthority = authority
            var currentPath = path
            `when`(uriMock.buildUpon()).thenReturn(builderMock)
            `when`(builderMock.authority(org.mockito.ArgumentMatchers.nullable(String::class.java))).thenAnswer { inv ->
                currentAuthority = inv.getArgument<String?>(0)
                builderMock
            }
            `when`(builderMock.path(org.mockito.ArgumentMatchers.nullable(String::class.java))).thenAnswer { inv ->
                currentPath = inv.getArgument<String?>(0)
                builderMock
            }
            `when`(builderMock.build()).thenAnswer {
                val reconstructedUrl = buildString {
                    if (scheme != null) append("$scheme://")
                    if (currentAuthority != null) append(currentAuthority)
                    if (currentPath != null) {
                        if (!currentPath!!.startsWith('/') && isNotEmpty()) append('/')
                        append(currentPath)
                    }
                    if (query != null) append("?$query")
                    if (fragment != null) append("#$fragment")
                }
                createMockUri(reconstructedUrl)
            }
            return uriMock
        }
    }

    private lateinit var handler: AppDeepLinkHandler

    @Before
    fun setUp() {
        val matchers = setOf(
            DeepLinkModule.provideChannelDeepLinkMatcher(),
            DeepLinkModule.provideChannelWithMessageDeepLinkMatcher(),
            DeepLinkModule.provideDmDeepLinkMatcher(),
            DeepLinkModule.provideDmWithMessageDeepLinkMatcher(),
            DeepLinkModule.provideShortcutDeepLinkMatcher(),
        )
        handler = AppDeepLinkHandler(matchers)
    }

    private fun createViewIntent(url: String?): Intent {
        val intent = mock(Intent::class.java)
        `when`(intent.action).thenReturn(Intent.ACTION_VIEW)
        if (url != null) {
            val uri = createMockUri(url)
            `when`(intent.data).thenReturn(uri)
        }
        return intent
    }

    @Test
    fun handleIntent_validChannelDeepLinkOnBareHost_returnsSyntheticBackStack() {
        val url = "https://${DeepLinkConstants.HOST_BASE}/chat/channels/general-123"
        val intent = createViewIntent(url)
        val backStack = handler.handleIntent(intent)

        assertNotNull(backStack)
        assertEquals(listOf(Home, ChannelDetail("general-123")), backStack)
    }

    @Test
    fun handleIntent_validChannelDeepLinkWithSubdomain_returnsSyntheticBackStack() {
        val url = "https://diginex.${DeepLinkConstants.HOST_BASE}/chat/channels/team-dev"
        val intent = createViewIntent(url)
        val backStack = handler.handleIntent(intent)

        assertNotNull(backStack)
        assertEquals(listOf(Home, ChannelDetail("team-dev")), backStack)
    }

    @Test
    fun handleIntent_validChannelDeepLinkWithMessageId_returnsSyntheticBackStack() {
        val url = "https://diginex.${DeepLinkConstants.HOST_BASE}/chat/channels/team-dev/msg-999"
        val intent = createViewIntent(url)
        val backStack = handler.handleIntent(intent)

        assertNotNull(backStack)
        assertEquals(listOf(Home, ChannelDetail("team-dev")), backStack)
    }

    @Test
    fun handleIntent_validDmDeepLink_returnsSyntheticBackStack() {
        val url = "https://${DeepLinkConstants.HOST_BASE}/chat/direct-messages/dm-456"
        val intent = createViewIntent(url)
        val backStack = handler.handleIntent(intent)

        assertNotNull(backStack)
        assertEquals(listOf(Home, DirectMessageDetail("dm-456")), backStack)
    }

    @Test
    fun handleIntent_profileShortcut_returnsSyntheticBackStack() {
        val intent = mock(Intent::class.java)
        val bundle = mock(Bundle::class.java)
        `when`(bundle.getString(AppShortcutManager.EXTRA_SHORTCUT_ACTION)).thenReturn(AppShortcutManager.ACTION_OPEN_PROFILE)
        `when`(intent.extras).thenReturn(bundle)

        val backStack = handler.handleIntent(intent)

        assertNotNull(backStack)
        assertEquals(listOf(Home, Profile), backStack)
    }

    @Test
    fun handleIntent_recentChannelShortcut_returnsSyntheticBackStack() {
        val intent = mock(Intent::class.java)
        val bundle = mock(Bundle::class.java)
        `when`(bundle.getString(AppShortcutManager.EXTRA_CHANNEL_ID)).thenReturn("channel-123")
        `when`(intent.extras).thenReturn(bundle)

        val backStack = handler.handleIntent(intent)

        assertNotNull(backStack)
        assertEquals(listOf(Home, ChannelDetail("channel-123")), backStack)
    }

    @Test
    fun handleIntent_customSchemeOrHttp_returnsNull() {
        assertNull(handler.handleIntent(createViewIntent("shine://channel/123")))
        assertNull(handler.handleIntent(createViewIntent("http://${DeepLinkConstants.HOST_BASE}/chat/channels/123")))
    }

    @Test
    fun handleIntent_foreignHost_returnsNull() {
        assertNull(handler.handleIntent(createViewIntent("https://random-site.com/chat/channels/123")))
    }

    @Test
    fun handleIntent_unsupportedPath_returnsNull() {
        assertNull(handler.handleIntent(createViewIntent("https://${DeepLinkConstants.HOST_BASE}/chat/random/123")))
        assertNull(handler.handleIntent(createViewIntent("https://${DeepLinkConstants.HOST_BASE}/unknown")))
    }

    @Test
    fun handleIntent_nullIntent_returnsNull() {
        assertNull(handler.handleIntent(null))
    }
}
