package com.example.shine.ui.navigation.deeplink

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.BackStackMatcher
import androidx.navigation3.runtime.deeplink.DeepLinkUri
import androidx.navigation3.runtime.deeplink.withBackStack
import com.example.shine.ui.navigation.ChannelDetail
import com.example.shine.ui.navigation.DirectMessageDetail
import com.example.shine.ui.navigation.Home
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object DeepLinkModule {

    @IntoSet
    @Provides
    fun provideChannelDeepLinkMatcher(): BackStackMatcher<*, NavKey> =
        AppDeepLinkMatcher(
            uriPattern = DeepLinkUri("${DeepLinkConstants.SCHEME_HTTPS}://${DeepLinkConstants.HOST_BASE}/${DeepLinkConstants.PATH_CHAT}/${DeepLinkConstants.PATH_CHANNELS}/{channelId}"),
            serializer = ChannelDetail.serializer(),
        ).withBackStack { matchResult ->
            listOf(Home, matchResult.key)
        }

    @IntoSet
    @Provides
    fun provideChannelWithMessageDeepLinkMatcher(): BackStackMatcher<*, NavKey> =
        AppDeepLinkMatcher(
            uriPattern = DeepLinkUri("${DeepLinkConstants.SCHEME_HTTPS}://${DeepLinkConstants.HOST_BASE}/${DeepLinkConstants.PATH_CHAT}/${DeepLinkConstants.PATH_CHANNELS}/{channelId}/.*"),
            serializer = ChannelDetail.serializer(),
        ).withBackStack { matchResult ->
            listOf(Home, matchResult.key)
        }

    @IntoSet
    @Provides
    fun provideDmDeepLinkMatcher(): BackStackMatcher<*, NavKey> =
        AppDeepLinkMatcher(
            uriPattern = DeepLinkUri("${DeepLinkConstants.SCHEME_HTTPS}://${DeepLinkConstants.HOST_BASE}/${DeepLinkConstants.PATH_CHAT}/${DeepLinkConstants.PATH_DIRECT_MESSAGES}/{dmId}"),
            serializer = DirectMessageDetail.serializer(),
        ).withBackStack { matchResult ->
            listOf(Home, matchResult.key)
        }

    @IntoSet
    @Provides
    fun provideDmWithMessageDeepLinkMatcher(): BackStackMatcher<*, NavKey> =
        AppDeepLinkMatcher(
            uriPattern = DeepLinkUri("${DeepLinkConstants.SCHEME_HTTPS}://${DeepLinkConstants.HOST_BASE}/${DeepLinkConstants.PATH_CHAT}/${DeepLinkConstants.PATH_DIRECT_MESSAGES}/{dmId}/.*"),
            serializer = DirectMessageDetail.serializer(),
        ).withBackStack { matchResult ->
            listOf(Home, matchResult.key)
        }

    @IntoSet
    @Provides
    fun provideShortcutDeepLinkMatcher(): BackStackMatcher<*, NavKey> =
        ShortcutDeepLinkMatcher().withBackStack { matchResult ->
            listOf(Home, matchResult.key)
        }
}
