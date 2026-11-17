package com.example.shine.feature.conversationsettings

import com.example.shine.core.common.ui.UiText
import com.example.shine.core.data.repository.ChannelRepository
import com.example.shine.core.data.repository.ConversationSettingRepository
import com.example.shine.core.data.repository.DirectMessageRepository
import com.example.shine.core.model.Channel
import com.example.shine.core.model.ConversationSetting
import com.example.shine.core.model.DirectMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationSettingsViewModelTest {

    private val channelRepository = FakeChannelRepository()
    private val directMessageRepository = FakeDirectMessageRepository()
    private val settingRepository = FakeConversationSettingRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadChannel_success_showsChannelInfo() = runTest {
        val viewModel = ConversationSettingsViewModel(
            conversationId = "channel-1",
            conversationType = ConversationType.CHANNEL,
            channelRepository = channelRepository,
            directMessageRepository = directMessageRepository,
            conversationSettingRepository = settingRepository,
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertEquals("general", state.title)
        assertEquals(
            UiText.Resource(R.string.conversation_settings_public_channel),
            state.subtitle,
        )
        assertEquals(ConversationType.CHANNEL, state.type)
        assertTrue(state.isNotificationEnabled)
        assertFalse(state.isLoading)
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

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertEquals("Alice Smith", state.title)
        assertNull(state.subtitle)
        assertEquals(ConversationType.DM, state.type)
        assertTrue(state.isNotificationEnabled)
        assertFalse(state.isLoading)
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

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertTrue(viewModel.uiState.value.isNotificationEnabled)

        viewModel.setNotificationEnabled(false)
        assertFalse(viewModel.uiState.value.isNotificationEnabled)

        viewModel.setNotificationEnabled(true)
        assertTrue(viewModel.uiState.value.isNotificationEnabled)
    }

    private class FakeChannelRepository : ChannelRepository {
        val channelFlow = MutableStateFlow<Channel?>(
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
        )

        override suspend fun getChannels(): Result<Unit> = Result.success(Unit)
        override suspend fun getRecentChannels(limit: Int): List<Channel> = emptyList()
        override suspend fun getChannel(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun createChannel(name: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteChannel(channelId: String): Result<Unit> = Result.success(Unit)
        override fun getChannelsFlow(limit: Int): Flow<List<Channel>> = emptyFlow()
        override fun getChannelFlow(id: String): Flow<Channel?> = channelFlow
        override suspend fun onChannelCreated(channel: Channel) {}
        override suspend fun clear() {}
    }

    private class FakeDirectMessageRepository : DirectMessageRepository {
        val dmFlow = MutableStateFlow<DirectMessage?>(
            DirectMessage(
                id = "dm-1",
                name = "Alice Smith",
                time = null,
                type = "direct",
            ),
        )

        override suspend fun getDirectMessages(): Result<Unit> = Result.success(Unit)
        override suspend fun getDirectMessage(id: String): Result<Unit> = Result.success(Unit)
        override fun getDirectMessagesFlow(): Flow<List<DirectMessage>> = emptyFlow()
        override fun getDirectMessageFlow(id: String): Flow<DirectMessage?> = dmFlow
        override suspend fun onDirectMessageCreated(message: DirectMessage) {}
        override suspend fun clear() {}
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
