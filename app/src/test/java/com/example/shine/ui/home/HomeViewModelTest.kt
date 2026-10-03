package com.example.shine.ui.home

import com.example.shine.R
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Channel
import com.example.shine.domain.model.Session
import com.example.shine.domain.model.User
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.domain.repository.ChannelRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val authRepository = FakeAuthRepository()
    private val channelRepository = FakeChannelRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun init_observesChannelsFromFlowAndLoadsRemote() = runTest {
        val viewModel = HomeViewModel(authRepository, channelRepository)
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

        val viewModel = HomeViewModel(authRepository, channelRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertEquals(0, state.channels.size)
        assertEquals(UiText.Resource(R.string.error_no_internet), state.errorMessage)
        assertFalse(state.isLoading)
    }

    private class FakeAuthRepository : AuthRepository {
        override val session: Flow<Session?> = MutableStateFlow(
            Session(
                token = "token",
                refreshToken = "refresh",
                expireAt = null,
                user = User(id = "1", email = "jane@example.com", firstName = "Jane", lastName = "Doe"),
            )
        )
        override suspend fun signIn(email: String, password: String): Session = throw NotImplementedError()
        override suspend fun signOut() {}
    }

    private class FakeChannelRepository : FakeChannelRepositoryBase()
}

open class FakeChannelRepositoryBase : ChannelRepository {
    val channelsFlow = MutableStateFlow(listOf(Channel("1", "general", false, false, true, null, null, null)))
    var shouldFail = false

    override fun getChannelsFlow(): Flow<List<Channel>> = channelsFlow

    override suspend fun getChannels(): List<Channel> {
        if (shouldFail) throw AppException(AppError.NO_INTERNET)
        return channelsFlow.value
    }

    override suspend fun getChannel(id: String): Channel = channelsFlow.value.first { it.id == id }
}
