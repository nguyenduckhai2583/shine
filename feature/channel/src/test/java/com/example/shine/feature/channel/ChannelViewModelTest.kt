package com.example.shine.feature.channel

import com.example.shine.core.common.ui.UiText
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.model.Channel
import com.example.shine.core.model.Session
import com.example.shine.core.testing.repository.FakeChannelRepository
import com.example.shine.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val initialChannels = listOf(
        Channel(
            id = "1",
            name = "general",
            isPrivate = false,
            isEncrypted = false,
            isDefault = true,
            categoryName = null,
            createdAt = null,
            lastActivityAt = null,
        ),
    )
    private val channelRepository = FakeChannelRepository(initialChannels)
    private val sessionFlow = MutableStateFlow<Session?>(Session(token = "tok", workspaceId = "ws-1"))
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        val sessionLocal = mock(SessionLocalDataSource::class.java)
        `when`(sessionLocal.session).thenReturn(sessionFlow)
        sessionManager = SessionManager(sessionLocal, CoroutineScope(mainDispatcherRule.testDispatcher))
    }

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

        assertEquals(1, viewModel.uiState.value.channels.size)
        assertEquals("general", viewModel.uiState.value.channels.first().name)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, channelRepository.getChannelsCallCount)
    }

    @Test
    fun refresh_success_updatesChannelsAndResetsIsRefreshing() = runTest {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.refresh()
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals(1, viewModel.uiState.value.channels.size)
        assertEquals(2, channelRepository.getChannelsCallCount)
    }

    @Test
    fun refresh_error_setsUserError() = runTest {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        channelRepository.shouldFail = true
        viewModel.refresh()

        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals(UiText.Raw("Failed to connect"), viewModel.uiState.value.userError)
    }

    @Test
    fun clearUserError_clearsError() = runTest {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        channelRepository.shouldFail = true
        viewModel.refresh()
        assertEquals(UiText.Raw("Failed to connect"), viewModel.uiState.value.userError)

        viewModel.clearUserError()
        assertNull(viewModel.uiState.value.userError)
    }

    @Test
    fun createChannel_callsRepositoryCreate() = runTest {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.createChannel("random")
        assertEquals(2, viewModel.uiState.value.channels.size)
        assertTrue(viewModel.uiState.value.channels.any { it.name == "random" })
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
}
