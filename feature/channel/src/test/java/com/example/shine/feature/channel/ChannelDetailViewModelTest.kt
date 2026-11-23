package com.example.shine.feature.channel

import app.cash.turbine.test
import com.example.shine.core.data.repository.ChannelRepository
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.model.Channel
import com.example.shine.core.model.Session
import com.example.shine.core.shortcut.FakeAppShortcutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelDetailViewModelTest {

    private val repository = FakeChannelRepository()
    private val shortcutManager = FakeAppShortcutManager()
    private val sessionFlow = MutableStateFlow<Session?>(
        Session(
            token = "token",
            refreshToken = "refresh",
            expireAt = null,
            user = null,
            workspaceId = "ws-1",
        )
    )
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val sessionLocal = mock(SessionLocalDataSource::class.java)
        `when`(sessionLocal.session).thenReturn(sessionFlow)
        sessionManager = SessionManager(sessionLocal, CoroutineScope(UnconfinedTestDispatcher()))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun load_success_showsChannelAndUpdatesShortcut() = runTest {
        val viewModel = ChannelDetailViewModel("1", repository, sessionManager, shortcutManager)
        viewModel.uiState.test {
            assertEquals("1", repository.lastId)
            assertEquals(ChannelDetailUiState.Success(CHANNEL), awaitItem())
            assertEquals("1", shortcutManager.lastUpdatedChannelId)
            assertEquals("general", shortcutManager.lastUpdatedChannelName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun channelNull_showsErrorUntilFetched() = runTest {
        repository.channelFlow.value = null
        repository.autoUpdateFlowOnGetChannel = false

        val viewModel = ChannelDetailViewModel("1", repository, sessionManager, shortcutManager)
        viewModel.uiState.test {
            assertEquals(ChannelDetailUiState.Error(), awaitItem())
            assertNull(shortcutManager.lastUpdatedChannelId)

            repository.channelFlow.value = CHANNEL

            assertEquals(ChannelDetailUiState.Success(CHANNEL), awaitItem())
            assertEquals("1", shortcutManager.lastUpdatedChannelId)
            assertEquals("general", shortcutManager.lastUpdatedChannelName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun offlineFirst_withCachedData_stillShowsChannel() = runTest {
        repository.channelFlow.value = CHANNEL

        val viewModel = ChannelDetailViewModel("1", repository, sessionManager, shortcutManager)
        viewModel.uiState.test {
            assertEquals(ChannelDetailUiState.Success(CHANNEL), awaitItem())
            assertEquals("1", shortcutManager.lastUpdatedChannelId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeChannelRepository : ChannelRepository {
        var lastId: String? = null
        var autoUpdateFlowOnGetChannel = true
        val channelFlow = MutableStateFlow<Channel?>(null)

        override suspend fun getChannels(workspaceId: String): Result<Unit> = Result.success(Unit)
        override suspend fun getRecentChannels(workspaceId: String, limit: Int): List<Channel> = emptyList()
        override suspend fun getChannel(id: String, workspaceId: String): Result<Unit> {
            lastId = id
            if (autoUpdateFlowOnGetChannel) {
                channelFlow.value = CHANNEL
            }
            return Result.success(Unit)
        }
        override suspend fun createChannel(name: String, workspaceId: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteChannel(channelId: String): Result<Unit> = Result.success(Unit)
        override fun getChannelsFlow(workspaceId: String, limit: Int): Flow<List<Channel>> = throw NotImplementedError()
        override fun getChannelFlow(id: String): Flow<Channel?> = channelFlow
        override suspend fun onChannelCreated(channel: Channel, workspaceId: String) {}
        override suspend fun clear() {}
    }

    companion object {
        private val CHANNEL = Channel(
            id = "1",
            name = "general",
            isPrivate = false,
            isEncrypted = false,
            isDefault = true,
            categoryName = null,
            createdAt = null,
            lastActivityAt = null,
        )
    }
}
