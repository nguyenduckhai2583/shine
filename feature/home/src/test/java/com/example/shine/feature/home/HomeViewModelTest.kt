package com.example.shine.feature.home

import app.cash.turbine.test
import com.example.shine.core.domain.FakeSignOutUseCase
import com.example.shine.core.model.Session
import com.example.shine.core.model.User
import com.example.shine.core.model.Workspace
import com.example.shine.core.shortcut.FakeAppShortcutManager
import com.example.shine.core.testing.repository.FakeAuthRepository
import com.example.shine.core.testing.repository.FakeUserRepository
import com.example.shine.core.testing.repository.FakeWorkspaceRepository
import com.example.shine.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository(
        initialSession = Session(
            token = "token",
            refreshToken = "refresh",
            expireAt = null,
            user = User(
                id = "1",
                email = "jane@example.com",
                firstName = "Jane",
                lastName = "Doe",
                avatarUrl = "https://example.com/avatar.jpg",
            ),
            workspaceId = "ws-1",
        ),
    )
    private val workspaceRepository = FakeWorkspaceRepository(
        initialWorkspaces = listOf(
            Workspace("ws-1", "Default Workspace", isActive = true, subdomain = "default"),
            Workspace("ws-2", "Second Workspace", isActive = true, subdomain = "second"),
        ),
        initialSelectedId = "ws-1",
    )
    private val userRepository = FakeUserRepository()
    private val shortcutManager = FakeAppShortcutManager()
    private val signOutUseCase = FakeSignOutUseCase(onSignOut = { authRepository.signedOut = true })

    @Before
    fun setUp() {
        workspaceRepository.onSelectWorkspace = {
            authRepository.sessionFlow.value = authRepository.sessionFlow.value?.copy(
                workspaceId = workspaceRepository.selectedIdFlow.value,
            )
        }
    }

    @Test
    fun init_observesSessionAndWorkspaces() = runTest {
        val viewModel = HomeViewModel(authRepository, workspaceRepository, userRepository, shortcutManager, signOutUseCase)
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("ws-1", state.selectedWorkspaceId)
            assertEquals("Default Workspace", state.currentWorkspaceName)
            assertEquals("https://example.com/avatar.jpg", state.avatarUrl)
            assertEquals("Jane Doe", state.displayName)
            assertEquals("jane@example.com", state.userEmail)
            assertEquals(2, state.workspaces.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectWorkspace_updatesWorkspace() = runTest {
        val viewModel = HomeViewModel(authRepository, workspaceRepository, userRepository, shortcutManager, signOutUseCase)
        viewModel.uiState.test {
            awaitItem()
            viewModel.selectWorkspace("ws-2")

            assertEquals("ws-2", workspaceRepository.selectedIdFlow.value)
            assertEquals(1, userRepository.getChatUsersCalls)
            assertEquals("ws-2", userRepository.lastWorkspaceId)
            assertEquals(1, shortcutManager.removeRecentCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectWorkspace_runsPreloadBeforeEmittingSelectedWorkspace() = runTest {
        var chatUsersCalled = false
        var selectedIdDuringPreload: String? = null
        userRepository.onGetChatUsers = {
            chatUsersCalled = true
            selectedIdDuringPreload = workspaceRepository.selectedIdFlow.value
        }

        val viewModel = HomeViewModel(authRepository, workspaceRepository, userRepository, shortcutManager, signOutUseCase)
        viewModel.uiState.test {
            awaitItem()
            viewModel.selectWorkspace("ws-2")

            assertTrue(chatUsersCalled)
            assertEquals("ws-1", selectedIdDuringPreload)
            assertEquals("ws-2", workspaceRepository.selectedIdFlow.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun signOut_invokesAuthRepositorySignOut() = runTest {
        val viewModel = HomeViewModel(authRepository, workspaceRepository, userRepository, shortcutManager, signOutUseCase)
        viewModel.signOut()

        assertTrue(signOutUseCase.signOutCalled)
        assertTrue(authRepository.signedOut)
        assertEquals(1, shortcutManager.removeAllCalls)
    }
}
