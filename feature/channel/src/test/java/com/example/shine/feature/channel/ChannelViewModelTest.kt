package com.example.shine.feature.channel

import com.example.shine.core.common.ui.UiText
import com.example.shine.core.data.repository.ChannelRepository
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.model.Channel
import com.example.shine.core.model.Session
import kotlinx.coroutines.CoroutineScope
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
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelViewModelTest {

    private val channelRepository = FakeChannelRepository()
    private val sessionFlow = MutableStateFlow<Session?>(Session(token = "tok", workspaceId = "ws-1"))
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
    fun initialState_hasLoadingTrue() {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun init_observesChannelsFromFlowAndLoadsRemote() = runTest {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertEquals(1, state.channels.size)
        assertEquals("general", state.channels[0].name)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.userError)
    }

    @Test
    fun init_whenFlowInitiallyEmpty_maintainsLoadingUntilRemoteFetchCompletes() = runTest {
        channelRepository.channelsFlow.value = emptyList()
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        assertTrue(viewModel.uiState.value.isLoading)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.channels.isEmpty())
    }

    @Test
    fun init_initialLoadError_setsUserError() = runTest {
        channelRepository.channelsFlow.value = emptyList()
        channelRepository.shouldFail = true
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(UiText("Failed to connect"), state.userError)
    }

    @Test
    fun refresh_success_updatesChannelsAndResetsIsRefreshing() = runTest {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.refresh()

        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertNull(state.userError)
        assertEquals(1, state.channels.size)
    }

    @Test
    fun refresh_error_setsUserErrorAndResetsIsRefreshing() = runTest {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        channelRepository.shouldFail = true
        viewModel.refresh()

        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertEquals(UiText("Failed to connect"), state.userError)
    }

    @Test
    fun workspaceChange_reloadsChannelsWithoutClearing() = runTest {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals(1, channelRepository.getChannelsCallCount)
        assertEquals("ws-1", viewModel.uiState.value.workspaceId)

        sessionFlow.value = Session(token = "tok", workspaceId = "ws-2")

        assertFalse(channelRepository.clearCalled)
        assertEquals(2, channelRepository.getChannelsCallCount)
        assertEquals("ws-2", viewModel.uiState.value.workspaceId)
    }

    @Test
    fun createChannel_success_createsChannel() = runTest {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.createChannel(name = "announcements")

        assertNull(viewModel.uiState.value.userError)
        val state = viewModel.uiState.value
        assertEquals(2, state.channels.size)
    }

    @Test
    fun createChannel_error_setsUserError() = runTest {
        channelRepository.shouldFail = true

        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.createChannel(name = "engineering")

        assertEquals(UiText("Failed to connect"), viewModel.uiState.value.userError)
    }

    @Test
    fun createChannel_blankName_doesNothing() = runTest {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.createChannel(name = "   ")
        val state = viewModel.uiState.value
        assertEquals(1, state.channels.size)
    }

    @Test
    fun clearUserError_clearsError() = runTest {
        channelRepository.shouldFail = true
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.createChannel(name = "general-2")
        assertEquals(UiText("Failed to connect"), viewModel.uiState.value.userError)

        viewModel.clearUserError()
        assertNull(viewModel.uiState.value.userError)
    }

    @Test
    fun deleteChannel_callsRepositoryDelete() = runTest {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.deleteChannel("1")
        assertEquals("1", channelRepository.deletedChannelId)
    }

    private class FakeChannelRepository : ChannelRepository {
        val channelsFlow = MutableStateFlow(listOf(Channel("1", "general", false,
            isEncrypted = false,
            isDefault = true,
            categoryName = null,
            createdAt = null,
            lastActivityAt = null
        )))
        var shouldFail = false
        var clearCalled = false
        var getChannelsCallCount = 0
        var deletedChannelId: String? = null

        override fun getChannelsFlow(workspaceId: String, limit: Int): Flow<List<Channel>> = channelsFlow

        override fun getChannelFlow(id: String): Flow<Channel?> = emptyFlow()

        override suspend fun getRecentChannels(workspaceId: String, limit: Int): List<Channel> = emptyList()

        override suspend fun onChannelCreated(channel: Channel, workspaceId: String) {}

        override suspend fun getChannels(workspaceId: String): Result<Unit> {
            getChannelsCallCount++
            if (shouldFail) return Result.failure(Exception("Failed to connect"))
            return Result.success(Unit)
        }

        override suspend fun getChannel(id: String, workspaceId: String): Result<Unit> = Result.success(Unit)

        override suspend fun createChannel(name: String, workspaceId: String): Result<Unit> {
            if (shouldFail) return Result.failure(Exception("Failed to connect"))
            val newChannel = Channel(
                id = "c-${System.currentTimeMillis()}",
                name = name,
                isPrivate = false,
                isEncrypted = false,
                isDefault = false,
                categoryName = null,
                createdAt = System.currentTimeMillis(),
                lastActivityAt = null,
            )
            channelsFlow.value += newChannel
            return Result.success(Unit)
        }

        override suspend fun deleteChannel(channelId: String): Result<Unit> {
            deletedChannelId = channelId
            channelsFlow.value = channelsFlow.value.filterNot { it.id == channelId }
            return Result.success(Unit)
        }

        override suspend fun clear() {
            clearCalled = true
            channelsFlow.value = emptyList()
        }
    }
}
