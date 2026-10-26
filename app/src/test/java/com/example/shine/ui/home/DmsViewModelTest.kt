package com.example.shine.ui.home

import com.example.shine.R
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.DirectMessage
import com.example.shine.domain.model.Workspace
import com.example.shine.domain.repository.DirectMessageRepository
import com.example.shine.domain.repository.WorkspaceRepository
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
    fun init_observesDmsFromFlowAndLoadsRemote() = runTest {
        val viewModel = DmsViewModel(dmRepository, workspaceRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertEquals(1, state.dms.size)
        assertEquals("Alice Smith", state.dms[0].name)
        assertFalse(state.isLoading)
    }

    @Test
    fun loadDms_error_updatesErrorMessage() = runTest {
        dmRepository.shouldFail = true
        dmRepository.dmsFlow.value = emptyList()

        val viewModel = DmsViewModel(dmRepository, workspaceRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertEquals(0, state.dms.size)
        assertEquals(UiText.Resource(R.string.error_no_internet), state.errorMessage)
        assertFalse(state.isLoading)
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

        override suspend fun getDirectMessages(): List<DirectMessage> {
            getDmsCallCount++
            if (shouldFail) throw AppException(AppError.NO_INTERNET)
            return dmsFlow.value
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

        override suspend fun fetchWorkspaces(): List<Workspace> = currentWorkspaces.value

        override suspend fun selectWorkspace(workspaceId: String) {
            currentSelectedId.value = workspaceId
        }

        override suspend fun clear() {
            currentWorkspaces.value = emptyList()
        }
    }
}
