package com.example.shine.feature.workspace

import app.cash.turbine.test
import com.example.shine.core.common.error.AppErrorEnum
import com.example.shine.core.common.error.AppException
import com.example.shine.core.data.repository.AuthRepository
import com.example.shine.core.data.repository.WorkspaceRepository
import com.example.shine.core.domain.FakeSignOutUseCase
import com.example.shine.core.model.Session
import com.example.shine.core.model.SignInStatus
import com.example.shine.core.model.Workspace
import com.example.shine.core.shortcut.FakeAppShortcutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceListViewModelTest {

    private val workspaceRepository = FakeWorkspaceRepository()
    private val authRepository = FakeAuthRepository()
    private val shortcutManager = FakeAppShortcutManager()
    private val signOutUseCase = FakeSignOutUseCase(onSignOut = { authRepository.signedOut = true })

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun init_loadsWorkspacesAndSetsSelected() = runTest {
        val viewModel = WorkspaceListViewModel(workspaceRepository, signOutUseCase, shortcutManager)
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.workspaces.size)
            assertEquals("ws-1", state.selectedWorkspaceId)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onSelectWorkspace_changesSelectedWorkspaceId() = runTest {
        val viewModel = WorkspaceListViewModel(workspaceRepository, signOutUseCase, shortcutManager)
        viewModel.uiState.test {
            awaitItem()
            viewModel.onSelectWorkspace("ws-2")
            assertEquals("ws-2", awaitItem().selectedWorkspaceId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmSelection_callsRepositorySelectWorkspace() = runTest {
        val viewModel = WorkspaceListViewModel(workspaceRepository, signOutUseCase, shortcutManager)
        viewModel.uiState.test {
            awaitItem()
            viewModel.onSelectWorkspace("ws-2")
            awaitItem()
            viewModel.confirmSelection()
            assertEquals("ws-2", workspaceRepository.lastSelectedId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun signOut_clearsShortcutsAndSignsOut() = runTest {
        val viewModel = WorkspaceListViewModel(workspaceRepository, signOutUseCase, shortcutManager)
        viewModel.signOut()

        assertTrue(shortcutManager.removeAllCalls > 0)
        assertTrue(signOutUseCase.signOutCalled)
        assertTrue(authRepository.signedOut)
    }

    @Test
    fun loadWorkspaces_failure_setsErrorMessage() = runTest {
        workspaceRepository.shouldFail = true
        val viewModel = WorkspaceListViewModel(workspaceRepository, signOutUseCase, shortcutManager)
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
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

        override suspend fun fetchWorkspaces(): Result<List<Workspace>> {
            if (shouldFail) return Result.failure(AppException(AppErrorEnum.NO_INTERNET))
            return Result.success(workspacesFlow.value)
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
        var signedOut = false
        override val session: Flow<Session?> = MutableStateFlow(null)
        override suspend fun signIn(email: String, password: String): Result<SignInStatus> = throw NotImplementedError()
        override suspend fun signOut() {
            signedOut = true
        }
    }
}
