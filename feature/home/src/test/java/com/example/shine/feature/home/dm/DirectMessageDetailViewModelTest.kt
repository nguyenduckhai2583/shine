package com.example.shine.feature.home.dm

import com.example.shine.core.common.R
import com.example.shine.core.common.error.AppErrorEnum
import com.example.shine.core.common.error.AppException
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.data.repository.DirectMessageRepository
import com.example.shine.core.model.DirectMessage
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
        repository.error = null

        val viewModel = DirectMessageDetailViewModel("1", repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals("1", repository.lastId)
        assertEquals(DirectMessageDetailUiState.Success(DM), viewModel.uiState.value)
    }

    @Test
    fun initialLocalDataNull_showsLoadingUntilFetched() = runTest {
        repository.dmFlow.value = null
        repository.autoUpdateFlowOnGetDirectMessage = false

        val viewModel = DirectMessageDetailViewModel("1", repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals(DirectMessageDetailUiState.Loading, viewModel.uiState.value)

        repository.dmFlow.value = DM
        assertEquals(DirectMessageDetailUiState.Success(DM), viewModel.uiState.value)
    }

    @Test
    fun load_failure_showsErrorThenRetrySucceeds() = runTest {
        repository.error = AppException(AppErrorEnum.NO_INTERNET)
        val viewModel = DirectMessageDetailViewModel("1", repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals(
            DirectMessageDetailUiState.Error(UiText.Resource(R.string.error_no_internet)),
            viewModel.uiState.value,
        )

        repository.error = null
        viewModel.load()

        assertEquals(DirectMessageDetailUiState.Success(DM), viewModel.uiState.value)
    }

    @Test
    fun offlineFirst_withCachedData_syncFailure_stillShowsDirectMessage() = runTest {
        repository.dmFlow.value = DM
        repository.error = AppException(AppErrorEnum.NO_INTERNET)

        val viewModel = DirectMessageDetailViewModel("1", repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals(DirectMessageDetailUiState.Success(DM), viewModel.uiState.value)
    }

    private class FakeDirectMessageRepository : DirectMessageRepository {
        var error: AppException? = null
        var lastId: String? = null
        var autoUpdateFlowOnGetDirectMessage = true
        val dmFlow = MutableStateFlow<DirectMessage?>(null)

        override suspend fun getDirectMessages(): Result<Unit> = Result.success(Unit)

        override suspend fun getDirectMessage(id: String): Result<Unit> {
            lastId = id
            val currentError = error
            return if (currentError != null) {
                Result.failure(currentError)
            } else {
                if (autoUpdateFlowOnGetDirectMessage) {
                    dmFlow.value = DM
                }
                Result.success(Unit)
            }
        }

        override fun getDirectMessageFlow(id: String): Flow<DirectMessage?> = dmFlow

        override fun getDirectMessagesFlow(): Flow<List<DirectMessage>> = emptyFlow()

        override suspend fun onDirectMessageCreated(message: DirectMessage) {}

        override suspend fun clear() {}
    }

    private companion object {
        val DM = DirectMessage(
            id = "1",
            name = "Alice Smith",
            time = 1_779_780_643L,
            type = "direct",
            createdAt = 1_779_700_000L,
            lastActivityAt = 1_779_780_643L,
        )
    }
}
