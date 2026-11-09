package com.example.shine.ui.home

import com.example.shine.core.shortcut.FakeAppShortcutManager
import com.example.shine.domain.model.Session
import com.example.shine.domain.model.SignInStatus
import com.example.shine.domain.model.User
import com.example.shine.domain.model.Workspace
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.domain.repository.UserRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val authRepository = FakeAuthRepository()
    private val workspaceRepository = FakeWorkspaceRepository()
    private val userRepository = FakeUserRepository()
    private val shortcutManager = FakeAppShortcutManager()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun init_observesSessionAndWorkspaces() = runTest {
        val viewModel = HomeViewModel(authRepository, workspaceRepository, userRepository, shortcutManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertEquals("ws-1", state.selectedWorkspaceId)
        assertEquals("Default Workspace", state.currentWorkspaceName)
        assertEquals("https://example.com/avatar.jpg", state.avatarUrl)
        assertEquals("Jane Doe", state.displayName)
        assertEquals("jane@example.com", state.userEmail)
        assertEquals(2, state.workspaces.size)
    }

    @Test
    fun selectWorkspace_updatesWorkspace() = runTest {
        val viewModel = HomeViewModel(authRepository, workspaceRepository, userRepository, shortcutManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.selectWorkspace("ws-2")

        assertEquals("ws-2", workspaceRepository.currentSelectedId.value)
        assertEquals(1, userRepository.getChatUsersCalls)
        assertEquals("ws-2", userRepository.lastWorkspaceId)
        assertEquals(1, shortcutManager.removeRecentCalls)
    }

    @Test
    fun selectWorkspace_runsPreloadBeforeEmittingSelectedWorkspace() = runTest {
        var chatUsersCalled = false
        var selectedIdDuringPreload: String? = null
        userRepository.onGetChatUsers = {
            chatUsersCalled = true
            selectedIdDuringPreload = workspaceRepository.currentSelectedId.value
        }

        val viewModel = HomeViewModel(authRepository, workspaceRepository, userRepository, shortcutManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.selectWorkspace("ws-2")

        assertTrue(chatUsersCalled)
        assertEquals("ws-1", selectedIdDuringPreload)
        assertEquals("ws-2", workspaceRepository.currentSelectedId.value)
    }

    @Test
    fun signOut_invokesAuthRepositorySignOut() = runTest {
        val viewModel = HomeViewModel(authRepository, workspaceRepository, userRepository, shortcutManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.signOut()

        assertTrue(authRepository.signedOut)
        assertEquals(1, shortcutManager.removeAllCalls)
    }

    private class FakeUserRepository : UserRepository {
        var getChatUsersCalls = 0
        var lastWorkspaceId: String? = null
        var onGetChatUsers: (() -> Unit)? = null

        override suspend fun getProfile(): Result<Unit> = Result.success(Unit)
        override suspend fun updateAvatar(uri: android.net.Uri): Result<User> = throw NotImplementedError()
        override suspend fun getChatUsers(workspaceId: String?): Result<Unit> {
            getChatUsersCalls++
            lastWorkspaceId = workspaceId
            onGetChatUsers?.invoke()
            return Result.success(Unit)
        }
    }

    private class FakeAuthRepository : AuthRepository {
        var signedOut = false
        val sessionFlow = MutableStateFlow(
            Session(
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
            )
        )
        override val session: Flow<Session?> = sessionFlow
        override suspend fun signIn(email: String, password: String): Result<SignInStatus> = throw NotImplementedError()
        override suspend fun signOut() {
            signedOut = true
        }
    }

    private class FakeWorkspaceRepository : WorkspaceRepository {
        var onSelectWorkspace: (() -> Unit)? = null
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
            onSelectWorkspace?.invoke()
            currentSelectedId.value = workspaceId
        }

        override suspend fun clear() {
            currentWorkspaces.value = emptyList()
        }
    }
}
