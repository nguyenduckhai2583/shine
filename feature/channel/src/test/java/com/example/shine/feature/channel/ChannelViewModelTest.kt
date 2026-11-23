package com.example.shine.feature.channel

import app.cash.turbine.test
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.model.Channel
import com.example.shine.core.model.Session
import com.example.shine.core.testing.repository.FakeChannelRepository
import com.example.shine.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

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
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.channels.size)
            assertEquals("general", state.channels.first().name)
            assertFalse(state.isLoading)
            assertEquals(1, channelRepository.getChannelsCallCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refresh_success_updatesChannelsAndResetsIsRefreshing() = runTest {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.isRefreshing)

            viewModel.refresh()

            assertTrue(awaitItem().isRefreshing)
            val refreshed = awaitItem()
            assertFalse(refreshed.isRefreshing)
            assertEquals(1, refreshed.channels.size)
            assertEquals(2, channelRepository.getChannelsCallCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refresh_error_setsUserError() = runTest {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        viewModel.uiState.test {
            awaitItem() // initial loaded state

            channelRepository.shouldFail = true
            viewModel.refresh()

            assertTrue(awaitItem().isRefreshing)
            val errorState = awaitItem()
            assertFalse(errorState.isRefreshing)
            assertEquals(UiText.Raw("Failed to connect"), errorState.userError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearUserError_clearsError() = runTest {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        viewModel.uiState.test {
            awaitItem() // initial loaded state

            channelRepository.shouldFail = true
            viewModel.refresh()

            assertTrue(awaitItem().isRefreshing)
            val errorState = awaitItem()
            assertEquals(UiText.Raw("Failed to connect"), errorState.userError)

            viewModel.clearUserError()
            val clearedState = awaitItem()
            assertNull(clearedState.userError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createChannel_callsRepositoryCreate() = runTest {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        viewModel.uiState.test {
            awaitItem() // initial loaded state

            viewModel.createChannel("random")

            val updatedState = awaitItem()
            assertEquals(2, updatedState.channels.size)
            assertTrue(updatedState.channels.any { it.name == "random" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteChannel_callsRepositoryDelete() = runTest {
        val viewModel = ChannelViewModel(channelRepository, sessionManager)
        viewModel.uiState.test {
            awaitItem() // initial loaded state

            viewModel.deleteChannel("1")

            val updatedState = awaitItem()
            assertEquals("1", channelRepository.deletedChannelId)
            assertEquals(0, updatedState.channels.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
