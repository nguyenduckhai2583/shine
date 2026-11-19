package com.example.shine.feature.dm

import com.example.shine.core.common.R
import com.example.shine.core.common.error.AppErrorEnum
import com.example.shine.core.common.error.AppException
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.data.repository.DirectMessageRepository
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.model.DirectMessage
import com.example.shine.core.model.Session
import kotlinx.coroutines.CoroutineScope
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class DmsViewModelTest {

    private val dmRepository = FakeDirectMessageRepository()
    private val sessionFlow = MutableStateFlow<Session?>(Session(token = "tok", workspaceId = "ws-1"))
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val sessionLocal = mock(SessionLocalDataSource::class.java)
        `when`(sessionLocal.session).thenReturn(sessionFlow)
        sessionManager = SessionManager(sessionLocal, CoroutineScope(UnconfinedTestDispatcher()))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun initialState_hasLoadingTrue() {
        val viewModel = DmsViewModel(dmRepository, sessionManager)
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun init_whenFlowInitiallyEmpty_maintainsLoadingUntilRemoteFetchCompletes() = runTest {
        dmRepository.dmsFlow.value = emptyList()
        val viewModel = DmsViewModel(dmRepository, sessionManager)
        assertTrue(viewModel.uiState.value.isLoading)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.dms.isEmpty())
    }

    @Test
    fun init_initialLoadError_setsUserError() = runTest {
        dmRepository.dmsFlow.value = emptyList()
        dmRepository.shouldFail = true
        val viewModel = DmsViewModel(dmRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(UiText(R.string.error_no_internet), state.userError)
    }

    @Test
    fun refresh_success_updatesDmsAndResetsIsRefreshing() = runTest {
        val viewModel = DmsViewModel(dmRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.refresh()

        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertNull(state.userError)
        assertEquals(1, state.dms.size)
    }

    @Test
    fun refresh_error_setsUserErrorAndResetsIsRefreshing() = runTest {
        val viewModel = DmsViewModel(dmRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        dmRepository.shouldFail = true
        viewModel.refresh()

        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertEquals(UiText(R.string.error_no_internet), state.userError)
    }

    @Test
    fun workspaceChange_reloadsDmsWithoutClearing() = runTest {
        val viewModel = DmsViewModel(dmRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals(1, dmRepository.getDmsCallCount)
        assertEquals("ws-1", viewModel.uiState.value.workspaceId)

        sessionFlow.value = Session(token = "tok", workspaceId = "ws-2")

        assertFalse(dmRepository.clearCalled)
        assertEquals(2, dmRepository.getDmsCallCount)
        assertEquals("ws-2", viewModel.uiState.value.workspaceId)
    }

    @Test
    fun clearUserError_clearsError() = runTest {
        dmRepository.shouldFail = true
        val viewModel = DmsViewModel(dmRepository, sessionManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.refresh()
        assertEquals(UiText(R.string.error_no_internet), viewModel.uiState.value.userError)

        viewModel.clearUserError()
        assertNull(viewModel.uiState.value.userError)
    }

    private class FakeDirectMessageRepository : DirectMessageRepository {
        val dmsFlow = MutableStateFlow(
            listOf(
                DirectMessage(
                    id = "1",
                    name = "Alice",
                    time = 123456789L,
                    type = "direct",
                )
            )
        )
        var shouldFail = false
        var clearCalled = false
        var getDmsCallCount = 0

        override fun getDirectMessagesFlow(): Flow<List<DirectMessage>> = dmsFlow

        override fun getDirectMessageFlow(id: String): Flow<DirectMessage?> = emptyFlow()

        override suspend fun onDirectMessageCreated(message: DirectMessage) {}

        override suspend fun getDirectMessages(): Result<Unit> {
            getDmsCallCount++
            if (shouldFail) return Result.failure(AppException(AppErrorEnum.NO_INTERNET))
            return Result.success(Unit)
        }

        override suspend fun getDirectMessage(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun clear() {
            clearCalled = true
            dmsFlow.value = emptyList()
        }
    }
}
