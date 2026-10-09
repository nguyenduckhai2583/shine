package com.example.shine.ui.workspace

import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Session
import com.example.shine.domain.model.Workspace
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.domain.repository.WorkspaceRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceListViewModelTest {

    private val workspaceRepository = FakeWorkspaceRepository()
    private val authRepository = FakeAuthRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun init_loadsWorkspacesAndSetsSelected() = runTest {
        val viewModel = WorkspaceListViewModel(workspaceRepository, authRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertEquals(2, state.workspaces.size)
        assertEquals("ws-1", state.selectedWorkspaceId)
        assertFalse(state.isLoading)
    }

    @Test
    fun onSelectWorkspace_changesSelectedWorkspaceId() = runTest {
        val viewModel = WorkspaceListViewModel(workspaceRepository, authRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onSelectWorkspace("ws-2")
        assertEquals("ws-2", viewModel.uiState.value.selectedWorkspaceId)
    }

    @Test
    fun confirmSelection_callsRepositorySelectWorkspace() = runTest {
        val viewModel = WorkspaceListViewModel(workspaceRepository, authRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.onSelectWorkspace("ws-2")
        viewModel.confirmSelection()

        assertEquals("ws-2", workspaceRepository.lastSelectedId)
    }

    @Test
    fun loadWorkspaces_failure_setsErrorMessage() = runTest {
        workspaceRepository.shouldFail = true
        val viewModel = WorkspaceListViewModel(workspaceRepository, authRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    private class FakeWorkspaceRepository : WorkspaceRepository {
        val workspacesFlow = MutableStateFlow(
            listOf(
                Workspace("ws-1", "Workspace 1", true),
                Workspace("ws-2", "Workspace 2", true),
            ),
        )
        val selectedIdFlow = MutableStateFlow<String?>(null)
        var lastSelectedId: String? = null
        var shouldFail = false

        override val workspaces: Flow<List<Workspace>> = workspacesFlow
        override val selectedWorkspaceId: Flow<String?> = selectedIdFlow

        override suspend fun fetchWorkspaces(): List<Workspace> {
            if (shouldFail) throw AppException(AppError.NO_INTERNET)
            return workspacesFlow.value
        }

        override suspend fun selectWorkspace(workspaceId: String) {
            lastSelectedId = workspaceId
            selectedIdFlow.value = workspaceId
        }

        override suspend fun clear() {
            workspacesFlow.value = emptyList()
        }
    }

    private class FakeAuthRepository : AuthRepository {
        override val session: Flow<Session?> = MutableStateFlow(null)
        override suspend fun signIn(email: String, password: String): Session = throw NotImplementedError()
        override suspend fun signOut() {}
    }
}
