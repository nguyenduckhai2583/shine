package com.example.shine.feature.dm

import app.cash.turbine.test
import com.example.shine.core.common.R
import com.example.shine.core.common.error.AppErrorEnum
import com.example.shine.core.common.error.AppException
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.model.DirectMessage
import com.example.shine.core.model.Session
import com.example.shine.core.testing.repository.FakeDirectMessageRepository
import com.example.shine.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class DmsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val initialDms = listOf(
        DirectMessage(
            id = "1",
            name = "Alice",
            time = 1000L,
            type = "direct",
        ),
    )
    private lateinit var dmRepository: FakeDirectMessageRepository
    private val sessionFlow = MutableStateFlow<Session?>(Session(token = "tok", workspaceId = "ws-1"))
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        dmRepository = FakeDirectMessageRepository(initialDms).apply {
            failureException = AppException(AppErrorEnum.NO_INTERNET)
        }
        val sessionLocal = mock(SessionLocalDataSource::class.java)
        `when`(sessionLocal.session).thenReturn(sessionFlow)
        sessionManager = SessionManager(sessionLocal, CoroutineScope(UnconfinedTestDispatcher()))
    }

    @Test
    fun initialState_hasLoadingTrue() {
        val viewModel = DmsViewModel(dmRepository, sessionManager)
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun init_whenFlowInitiallyEmpty_maintainsLoadingUntilRemoteFetchCompletes() {
        dmRepository.dmsFlow.value = emptyList()
        val viewModel = DmsViewModel(dmRepository, sessionManager)

        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.dms.isEmpty())
    }

    @Test
    fun init_observesDmsFromFlowAndLoadsRemote() = runTest {
        val viewModel = DmsViewModel(dmRepository, sessionManager)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.dms.size)
            assertEquals("Alice", state.dms.first().name)
            assertFalse(state.isLoading)
            assertEquals(1, dmRepository.getDmsCallCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refresh_success_updatesIsRefreshing() = runTest {
        val viewModel = DmsViewModel(dmRepository, sessionManager)

        viewModel.uiState.test {
            awaitItem()

            viewModel.refresh()

            assertTrue(awaitItem().isRefreshing)
            val state = awaitItem()
            assertFalse(state.isRefreshing)
            assertEquals(2, dmRepository.getDmsCallCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refresh_error_setsUserError() = runTest {
        val viewModel = DmsViewModel(dmRepository, sessionManager)

        viewModel.uiState.test {
            awaitItem()

            dmRepository.shouldFail = true
            viewModel.refresh()

            assertTrue(awaitItem().isRefreshing)
            val errorState = awaitItem()
            assertFalse(errorState.isRefreshing)
            assertEquals(UiText(R.string.error_no_internet), errorState.userError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearUserError_clearsErrorState() = runTest {
        dmRepository.shouldFail = true
        val viewModel = DmsViewModel(dmRepository, sessionManager)
        viewModel.uiState.test {
            val errorState = awaitItem()
            assertEquals(UiText(R.string.error_no_internet), errorState.userError)

            viewModel.clearUserError()
            val clearedState = awaitItem()
            assertNull(clearedState.userError)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
