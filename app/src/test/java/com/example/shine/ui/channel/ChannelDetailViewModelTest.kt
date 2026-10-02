package com.example.shine.ui.channel

import com.example.shine.domain.model.AuthException
import com.example.shine.domain.model.Channel
import com.example.shine.domain.repository.ChannelRepository
import com.example.shine.domain.usecase.GetChannelUseCase
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
        repository.result = Result.success(CHANNEL)

        val viewModel = ChannelDetailViewModel("1", GetChannelUseCase(repository))

        assertEquals("1", repository.lastId)
        assertEquals(CHANNEL, viewModel.uiState.value.channel)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun load_failure_showsErrorThenRetrySucceeds() {
        repository.result = Result.failure(AuthException.Network())
        val viewModel = ChannelDetailViewModel("1", GetChannelUseCase(repository))
        assertEquals("No internet connection", viewModel.uiState.value.errorMessage)

        repository.result = Result.success(CHANNEL)
        viewModel.load()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(CHANNEL, viewModel.uiState.value.channel)
    }

    private class FakeChannelRepository : ChannelRepository {
        var result: Result<Channel> = Result.success(CHANNEL)
        var lastId: String? = null

        override suspend fun getChannels(): Result<List<Channel>> = Result.success(listOf(CHANNEL))

        override suspend fun getChannel(id: String): Result<Channel> {
            lastId = id
            return result
        }
    }

    private companion object {
        val CHANNEL = Channel("1", "general", false, false, true, null, 1L, 2L)
    }
}
