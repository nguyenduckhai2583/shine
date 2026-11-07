package com.example.shine.ui.channel

import com.example.shine.R
import com.example.shine.core.shortcut.FakeAppShortcutManager
import com.example.shine.domain.model.AppErrorEnum
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Channel
import com.example.shine.domain.repository.ChannelRepository
import com.example.shine.ui.common.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun load_success_showsChannelAndUpdatesShortcut() {
        repository.error = null

        val viewModel = ChannelDetailViewModel("1", repository, shortcutManager)

        assertEquals("1", repository.lastId)
        assertEquals(CHANNEL, viewModel.uiState.value.channel)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("1", shortcutManager.lastUpdatedChannelId)
        assertEquals("general", shortcutManager.lastUpdatedChannelName)
    }

    @Test
    fun load_failure_showsErrorThenRetrySucceeds() {
        repository.error = AppException(AppErrorEnum.NO_INTERNET)
        val viewModel = ChannelDetailViewModel("1", repository, shortcutManager)
        assertEquals(UiText.Resource(R.string.error_no_internet), viewModel.uiState.value.errorMessage)
        assertNull(shortcutManager.lastUpdatedChannelId)

        repository.error = null
        viewModel.load()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(CHANNEL, viewModel.uiState.value.channel)
        assertEquals("1", shortcutManager.lastUpdatedChannelId)
    }

    private class FakeChannelRepository : ChannelRepository {
        var error: AppException? = null
        var lastId: String? = null

        override suspend fun getChannels(): Result<List<Channel>> = Result.success(listOf(CHANNEL))

        override suspend fun getChannel(id: String): Result<Channel> {
            lastId = id
            val currentError = error
            return if (currentError != null) {
                Result.failure(currentError)
            } else {
                Result.success(CHANNEL)
            }
        }

        override suspend fun createChannel(name: String): Result<Unit> = Result.success(Unit)
    }

    private companion object {
        val CHANNEL = Channel("1", "general", false, false, true, null, 1L, 2L)
    }
}
