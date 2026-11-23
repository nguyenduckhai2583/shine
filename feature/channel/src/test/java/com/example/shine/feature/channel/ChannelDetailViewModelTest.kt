package com.example.shine.feature.channel

import app.cash.turbine.test
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.model.Channel
import com.example.shine.core.model.Session
import com.example.shine.core.shortcut.FakeAppShortcutManager
import com.example.shine.core.testing.repository.FakeChannelRepository
import com.example.shine.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class ChannelDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeChannelRepository()
    private val shortcutManager = FakeAppShortcutManager()
    private val sessionFlow = MutableStateFlow<Session?>(
        Session(
            token = "token",
            refreshToken = "refresh",
            expireAt = null,
            user = null,
            workspaceId = "ws-1",
        ),
    )
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        val sessionLocal = mock(SessionLocalDataSource::class.java)
        `when`(sessionLocal.session).thenReturn(sessionFlow)
        sessionManager = SessionManager(sessionLocal, CoroutineScope(mainDispatcherRule.testDispatcher))
    }

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
