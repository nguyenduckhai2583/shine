package com.example.shine.ui.channel

import com.example.shine.R
import com.example.shine.domain.model.AppError
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

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun load_success_showsChannel() {
        repository.error = null

        val viewModel = ChannelDetailViewModel("1", repository)

        assertEquals("1", repository.lastId)
        assertEquals(CHANNEL, viewModel.uiState.value.channel)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun load_failure_showsErrorThenRetrySucceeds() {
        repository.error = AppException(AppError.NO_INTERNET)
        val viewModel = ChannelDetailViewModel("1", repository)
        assertEquals(UiText.Resource(R.string.error_no_internet), viewModel.uiState.value.errorMessage)

        repository.error = null
        viewModel.load()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(CHANNEL, viewModel.uiState.value.channel)
    }

    private class FakeChannelRepository : ChannelRepository {
        var error: AppException? = null
        var lastId: String? = null

        override suspend fun getChannels(): List<Channel> = listOf(CHANNEL)

        override suspend fun getChannel(id: String): Channel {
            lastId = id
            error?.let { throw it }
            return CHANNEL
        }
    }

    private companion object {
        val CHANNEL = Channel("1", "general", false, false, true, null, 1L, 2L)
    }
}
