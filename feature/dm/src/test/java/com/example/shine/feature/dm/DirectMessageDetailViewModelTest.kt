package com.example.shine.feature.dm

import app.cash.turbine.test
import com.example.shine.core.data.repository.DirectMessageRepository
import com.example.shine.core.model.DirectMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DirectMessageDetailViewModelTest {

    private val repository = FakeDirectMessageRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun load_success_showsDirectMessage() = runTest {
        val viewModel = DirectMessageDetailViewModel("1", repository)
        viewModel.uiState.test {
            assertEquals("1", repository.lastId)
            assertEquals(DirectMessageDetailUiState.Success(DM), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dmNull_showsErrorUntilFetched() = runTest {
        repository.dmFlow.value = null
        repository.autoUpdateFlowOnGetDirectMessage = false

        val viewModel = DirectMessageDetailViewModel("1", repository)
        viewModel.uiState.test {
            assertEquals(DirectMessageDetailUiState.Error(), awaitItem())

            repository.dmFlow.value = DM
            assertEquals(DirectMessageDetailUiState.Success(DM), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun offlineFirst_withCachedData_stillShowsDirectMessage() = runTest {
        repository.dmFlow.value = DM

        val viewModel = DirectMessageDetailViewModel("1", repository)
        viewModel.uiState.test {
            assertEquals(DirectMessageDetailUiState.Success(DM), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeDirectMessageRepository : DirectMessageRepository {
        var lastId: String? = null
        var autoUpdateFlowOnGetDirectMessage = true
        val dmFlow = MutableStateFlow<DirectMessage?>(null)

        override suspend fun getDirectMessages(): Result<Unit> = Result.success(Unit)
        override suspend fun getDirectMessage(id: String): Result<Unit> {
            lastId = id
            if (autoUpdateFlowOnGetDirectMessage) {
                dmFlow.value = DM
            }
            return Result.success(Unit)
        }
        override fun getDirectMessagesFlow(): Flow<List<DirectMessage>> = emptyFlow()
        override fun getDirectMessageFlow(id: String): Flow<DirectMessage?> = dmFlow
        override suspend fun onDirectMessageCreated(message: DirectMessage) {}
        override suspend fun clear() {}
    }

    companion object {
        private val DM = DirectMessage(
            id = "1",
            name = "Alice",
            time = null,
            type = "direct",
        )
    }
}
