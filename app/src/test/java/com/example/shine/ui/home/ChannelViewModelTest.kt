package com.example.shine.ui.home

import com.example.shine.R
import com.example.shine.domain.model.Channel
import com.example.shine.domain.model.Workspace
import com.example.shine.domain.repository.ChannelRepository
import com.example.shine.domain.repository.WorkspaceRepository
import com.example.shine.ui.common.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class ChannelViewModelTest {

    private val channelRepository = FakeChannelRepository()
    private val workspaceRepository = FakeWorkspaceRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun init_observesChannelsFromFlowAndLoadsRemote() = runTest {
        val viewModel = ChannelViewModel(channelRepository, workspaceRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertEquals(1, state.channels.size)
        assertEquals("general", state.channels[0].name)
        assertFalse(state.isLoading)
    }

    @Test
    fun loadChannels_error_updatesErrorMessage() = runTest {
        channelRepository.shouldFail = true
        channelRepository.channelsFlow.value = emptyList()

        val viewModel = ChannelViewModel(channelRepository, workspaceRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertEquals(0, state.channels.size)
        assertEquals(UiText("Failed to connect"), state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun workspaceChange_clearsAndReloadsChannels() = runTest {
        val viewModel = ChannelViewModel(channelRepository, workspaceRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals(1, channelRepository.getChannelsCallCount)

        workspaceRepository.currentSelectedId.value = "ws-2"

        assertTrue(channelRepository.clearCalled)
        assertEquals(2, channelRepository.getChannelsCallCount)
    }

    @Test
    fun createChannel_success_createsChannel_andSetsSuccessSnackbar() = runTest {
        val viewModel = ChannelViewModel(channelRepository, workspaceRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.createChannel(name = "announcements")

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(UiText(R.string.channel_created_success, "#announcements"), viewModel.uiState.value.snackbarMessage)
        assertEquals(2, viewModel.uiState.value.channels.size)
    }

    @Test
    fun createChannel_error_setsErrorSnackbar() = runTest {
        channelRepository.shouldFail = true

        val viewModel = ChannelViewModel(channelRepository, workspaceRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.createChannel(name = "engineering")

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(UiText("Failed to connect"), viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun createChannel_blankName_doesNothing() = runTest {
        val viewModel = ChannelViewModel(channelRepository, workspaceRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.createChannel(name = "   ")
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.channels.size)
    }

    @Test
    fun clearSnackbarMessage_clearsSnackbar() = runTest {
        val viewModel = ChannelViewModel(channelRepository, workspaceRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.createChannel(name = "general-2")
        assertEquals(UiText(R.string.channel_created_success, "#general-2"), viewModel.uiState.value.snackbarMessage)

        viewModel.clearSnackbarMessage()
        assertNull(viewModel.uiState.value.snackbarMessage)
    }

    private class FakeChannelRepository : ChannelRepository {
        val channelsFlow = MutableStateFlow(listOf(Channel("1", "general", false, false, true, null, null, null)))
        var shouldFail = false
        var clearCalled = false
        var getChannelsCallCount = 0

        override fun getChannelsFlow(): Flow<List<Channel>> = channelsFlow

        override suspend fun getChannels(): Result<List<Channel>> {
            getChannelsCallCount++
            if (shouldFail) return Result.failure(Exception("Failed to connect"))
            return Result.success(channelsFlow.value)
        }

        override suspend fun getChannel(id: String): Result<Channel> =
            Result.success(channelsFlow.value.first { it.id == id })

        override suspend fun createChannel(name: String): Result<Unit> {
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
            channelsFlow.value = channelsFlow.value + newChannel
            return Result.success(Unit)
        }

        override fun clear() {
            clearCalled = true
            channelsFlow.value = emptyList()
        }
    }

    private class FakeWorkspaceRepository : WorkspaceRepository {
        val currentWorkspaces = MutableStateFlow(
            listOf(
                Workspace("ws-1", "Default Workspace", true, "default"),
                Workspace("ws-2", "Second Workspace", true, "second"),
            ),
        )
        val currentSelectedId = MutableStateFlow<String?>("ws-1")

        override val workspaces: Flow<List<Workspace>> = currentWorkspaces
        override val selectedWorkspaceId: Flow<String?> = currentSelectedId

        override suspend fun fetchWorkspaces(): Result<List<Workspace>> =
            Result.success(currentWorkspaces.value)

        override suspend fun selectWorkspace(workspaceId: String) {
            currentSelectedId.value = workspaceId
        }

        override suspend fun clear() {
            currentWorkspaces.value = emptyList()
        }
    }
}
