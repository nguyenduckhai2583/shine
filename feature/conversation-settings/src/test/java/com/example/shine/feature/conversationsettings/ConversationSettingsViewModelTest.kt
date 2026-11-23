package com.example.shine.feature.conversationsettings

import app.cash.turbine.test
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.data.repository.ConversationSettingRepository
import com.example.shine.core.model.Channel
import com.example.shine.core.model.ConversationSetting
import com.example.shine.core.model.DirectMessage
import com.example.shine.core.testing.repository.FakeChannelRepository
import com.example.shine.core.testing.repository.FakeDirectMessageRepository
import com.example.shine.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConversationSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val channelRepository = FakeChannelRepository(
        initialChannels = listOf(
            Channel(
                id = "channel-1",
                name = "general",
                isPrivate = false,
                isEncrypted = false,
                isDefault = true,
                categoryName = null,
                createdAt = null,
                lastActivityAt = null,
            ),
        ),
    )
    private val directMessageRepository = FakeDirectMessageRepository(
        initialDms = listOf(
            DirectMessage(
                id = "dm-1",
                name = "Alice Smith",
                time = null,
                type = "direct",
            ),
        ),
    )
    private val settingRepository = FakeConversationSettingRepository()

    @Test
    fun loadChannel_success_showsChannelInfo() = runTest {
        val viewModel = ConversationSettingsViewModel(
            conversationId = "channel-1",
            conversationType = ConversationType.CHANNEL,
            channelRepository = channelRepository,
            directMessageRepository = directMessageRepository,
            conversationSettingRepository = settingRepository,
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("general", state.title)
            assertEquals(
                UiText.Resource(R.string.conversation_settings_public_channel),
                state.subtitle,
            )
            assertEquals(ConversationType.CHANNEL, state.type)
            assertTrue(state.isNotificationEnabled)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadDm_success_showsDmInfo() = runTest {
        val viewModel = ConversationSettingsViewModel(
            conversationId = "dm-1",
            conversationType = ConversationType.DM,
            channelRepository = channelRepository,
            directMessageRepository = directMessageRepository,
            conversationSettingRepository = settingRepository,
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Alice Smith", state.title)
            assertNull(state.subtitle)
            assertEquals(ConversationType.DM, state.type)
            assertTrue(state.isNotificationEnabled)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setNotificationEnabled_updatesState() = runTest {
        val viewModel = ConversationSettingsViewModel(
            conversationId = "channel-1",
            conversationType = ConversationType.CHANNEL,
            channelRepository = channelRepository,
            directMessageRepository = directMessageRepository,
            conversationSettingRepository = settingRepository,
        )

        viewModel.uiState.test {
            assertTrue(awaitItem().isNotificationEnabled)

            viewModel.setNotificationEnabled(false)
            assertFalse(awaitItem().isNotificationEnabled)

            viewModel.setNotificationEnabled(true)
            assertTrue(awaitItem().isNotificationEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeConversationSettingRepository : ConversationSettingRepository {
        private val settings = mutableMapOf<String, MutableStateFlow<ConversationSetting?>>()

        override fun getConversationSettingFlow(conversationId: String): Flow<ConversationSetting?> =
            settings.getOrPut(conversationId) { MutableStateFlow(null) }

        override suspend fun setNotificationEnabled(conversationId: String, enabled: Boolean): Result<Unit> {
            val flow = settings.getOrPut(conversationId) { MutableStateFlow(null) }
            flow.value = ConversationSetting(conversationId, enabled)
            return Result.success(Unit)
        }
    }
}
