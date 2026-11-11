package com.example.shine.feature.home

import com.example.shine.core.common.R
import com.example.shine.core.common.error.AppErrorEnum
import com.example.shine.core.common.error.AppException
import com.example.shine.core.model.DirectMessage
import com.example.shine.core.model.Workspace
import com.example.shine.core.data.repository.DirectMessageRepository
import com.example.shine.core.data.repository.WorkspaceRepository
import com.example.shine.core.common.ui.UiText
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DmsViewModelTest {

    private val dmRepository = FakeDirectMessageRepository()
    private val workspaceRepository = FakeWorkspaceRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun initialState_hasLoadingTrue() {
        val viewModel = DmsViewModel(dmRepository, workspaceRepository)
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun init_whenFlowInitiallyEmpty_maintainsLoadingUntilRemoteFetchCompletes() = runTest {
        dmRepository.dmsFlow.value = emptyList()
        val viewModel = DmsViewModel(dmRepository, workspaceRepository)
        assertTrue(viewModel.uiState.value.isLoading)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.dms.isEmpty())
    }

    @Test
    fun init_observesDmsFromFlowAndLoadsRemote() = runTest {
        val viewModel = DmsViewModel(dmRepository, workspaceRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertEquals(1, state.dms.size)
        assertEquals("Alice Smith", state.dms[0].name)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.userError)
    }

    @Test
    fun refresh_success_updatesDmsAndResetsIsRefreshing() = runTest {
        val viewModel = DmsViewModel(dmRepository, workspaceRepository)
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
        val viewModel = DmsViewModel(dmRepository, workspaceRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        dmRepository.shouldFail = true
        viewModel.refresh()

        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertEquals(UiText.Resource(R.string.error_no_internet), state.userError)

        viewModel.clearUserError()
        assertNull(viewModel.uiState.value.userError)
    }

    @Test
    fun workspaceChange_clearsAndReloadsDms() = runTest {
        val viewModel = DmsViewModel(dmRepository, workspaceRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertEquals(1, dmRepository.getDmsCallCount)

        workspaceRepository.currentSelectedId.value = "ws-2"

        assertTrue(dmRepository.clearCalled)
        assertEquals(2, dmRepository.getDmsCallCount)
    }

    private class FakeDirectMessageRepository : DirectMessageRepository {
        val dmsFlow = MutableStateFlow(listOf(DirectMessage("1", "Alice Smith", 1000L)))
        var shouldFail = false
        var clearCalled = false
        var getDmsCallCount = 0

        override fun getDirectMessagesFlow(): Flow<List<DirectMessage>> = dmsFlow

        override suspend fun getDirectMessages(): Result<Unit> {
            getDmsCallCount++
            return if (shouldFail) {
                Result.failure(AppException(AppErrorEnum.NO_INTERNET))
            } else {
                Result.success(Unit)
            }
        }

        override fun clear() {
            clearCalled = true
            dmsFlow.value = emptyList()
        }
    }

    private class FakeWorkspaceRepository : WorkspaceRepository {
        val currentWorkspaces = MutableStateFlow(
            listOf(
                Workspace("ws-1", "Default Workspace", true, "default"),
                Workspace("ws-2", "Second Workspace", true, "second"),
            ),
        )
        val currentSelectedId = MutableStateFlow<String?>("ws-1")

        override val workspaces: Flow<List<Workspace>> = currentWorkspaces
        override val selectedWorkspaceId: Flow<String?> = currentSelectedId

        override suspend fun fetchWorkspaces(): Result<List<Workspace>> =
            Result.success(currentWorkspaces.value)

        override suspend fun selectWorkspace(workspaceId: String) {
            currentSelectedId.value = workspaceId
        }

        override suspend fun clear() {
            currentWorkspaces.value = emptyList()
        }
    }
}
