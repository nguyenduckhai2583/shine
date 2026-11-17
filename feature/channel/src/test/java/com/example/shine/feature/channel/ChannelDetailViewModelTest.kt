package com.example.shine.feature.channel

import com.example.shine.core.data.repository.ChannelRepository
import com.example.shine.core.model.Channel
import com.example.shine.core.shortcut.FakeAppShortcutManager
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelDetailViewModelTest {

    private val repository = FakeChannelRepository()
    private val shortcutManager = FakeAppShortcutManager()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun load_success_showsChannelAndUpdatesShortcut() = runTest {
        val viewModel = ChannelDetailViewModel("1", repository, shortcutManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals("1", repository.lastId)
        assertEquals(ChannelDetailUiState.Success(CHANNEL), viewModel.uiState.value)
        assertEquals("1", shortcutManager.lastUpdatedChannelId)
        assertEquals("general", shortcutManager.lastUpdatedChannelName)
    }

    @Test
    fun channelNull_showsErrorUntilFetched() = runTest {
        repository.channelFlow.value = null
        repository.autoUpdateFlowOnGetChannel = false

        val viewModel = ChannelDetailViewModel("1", repository, shortcutManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals(ChannelDetailUiState.Error(), viewModel.uiState.value)
        assertNull(shortcutManager.lastUpdatedChannelId)

        repository.channelFlow.value = CHANNEL

        assertEquals(ChannelDetailUiState.Success(CHANNEL), viewModel.uiState.value)
        assertEquals("1", shortcutManager.lastUpdatedChannelId)
        assertEquals("general", shortcutManager.lastUpdatedChannelName)
    }

    @Test
    fun offlineFirst_withCachedData_stillShowsChannel() = runTest {
        repository.channelFlow.value = CHANNEL

        val viewModel = ChannelDetailViewModel("1", repository, shortcutManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals(ChannelDetailUiState.Success(CHANNEL), viewModel.uiState.value)
        assertEquals("1", shortcutManager.lastUpdatedChannelId)
    }

    private class FakeChannelRepository : ChannelRepository {
        var lastId: String? = null
        var autoUpdateFlowOnGetChannel = true
        val channelFlow = MutableStateFlow<Channel?>(null)

        override suspend fun getChannels(): Result<Unit> = Result.success(Unit)

        override suspend fun getChannel(id: String): Result<Unit> {
            lastId = id
            if (autoUpdateFlowOnGetChannel) {
                channelFlow.value = CHANNEL
            }
            return Result.success(Unit)
        }

        override fun getChannelFlow(id: String): Flow<Channel?> = channelFlow

        override suspend fun getRecentChannels(limit: Int): List<Channel> = emptyList()

        override suspend fun createChannel(name: String): Result<Unit> = Result.success(Unit)

        override suspend fun deleteChannel(channelId: String): Result<Unit> = Result.success(Unit)

        override fun getChannelsFlow(limit: Int): Flow<List<Channel>> = emptyFlow()

        override suspend fun onChannelCreated(channel: Channel) {}

        override suspend fun clear() {}
    }

    private companion object {
        val CHANNEL = Channel(
            id = "1",
            name = "general",
            isPrivate = false,
            isEncrypted = false,
            isDefault = true,
            categoryName = null,
            createdAt = 1L,
            lastActivityAt = 2L,
        )
    }
}
