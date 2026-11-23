package com.example.shine.feature.workspace

import app.cash.turbine.test
import com.example.shine.core.common.error.AppErrorEnum
import com.example.shine.core.common.error.AppException
import com.example.shine.core.domain.FakeSignOutUseCase
import com.example.shine.core.shortcut.FakeAppShortcutManager
import com.example.shine.core.testing.repository.FakeAuthRepository
import com.example.shine.core.testing.repository.FakeWorkspaceRepository
import com.example.shine.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkspaceListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val workspaceRepository = FakeWorkspaceRepository().apply {
        failureException = AppException(AppErrorEnum.NO_INTERNET)
    }
    private val authRepository = FakeAuthRepository()
    private val shortcutManager = FakeAppShortcutManager()
    private val signOutUseCase = FakeSignOutUseCase(onSignOut = { authRepository.signedOut = true })

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
    fun confirmSelection_callsSelectWorkspace() = runTest {
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
    fun signOut_invokesSignOutUseCase() = runTest {
        val viewModel = WorkspaceListViewModel(workspaceRepository, signOutUseCase, shortcutManager)
        viewModel.signOut()
        assertTrue(authRepository.signedOut)
        assertEquals(1, shortcutManager.removeAllCalls)
    }

    @Test
    fun fetchWorkspaces_error_setsErrorMessage() = runTest {
        workspaceRepository.shouldFail = true
        val viewModel = WorkspaceListViewModel(workspaceRepository, signOutUseCase, shortcutManager)
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
