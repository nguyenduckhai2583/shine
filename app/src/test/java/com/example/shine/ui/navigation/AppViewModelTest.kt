package com.example.shine.ui.navigation

import android.content.Intent
import android.net.Uri
import com.example.shine.core.shortcut.AppShortcutManager
import com.example.shine.core.common.network.NetworkMonitor
import com.example.shine.core.model.Session
import com.example.shine.core.model.SignInStatus
import com.example.shine.core.model.User
import com.example.shine.core.data.repository.AuthRepository
import com.example.shine.core.data.repository.UserRepository
import com.example.shine.ui.navigation.deeplink.DeepLinkDestination
import com.example.shine.ui.navigation.deeplink.DeepLinkParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.coroutines.resume

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val sessionFlow = MutableStateFlow<Session?>(null)
    private val authRepository = FakeAuthRepository(sessionFlow)
    private val userRepository = FakeUserRepository()
    private val deepLinkParser = DeepLinkParser()
    private val networkMonitor = FakeNetworkMonitor()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun activeSession_fetchesUserProfileAndChatUsersOnLaunch() = runTest {
        val testSession = Session(
            token = "jwt-token",
            workspaceId = "workspace-1",
            user = User(id = "user-1", email = "user@test.com", firstName = "Jane", lastName = "Doe"),
        )
        sessionFlow.value = testSession

        val viewModel = AppViewModel(authRepository, userRepository, deepLinkParser, networkMonitor)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.authState.collect {}
        }

        assertEquals(1, userRepository.getProfileCalls)
        assertEquals(1, userRepository.getChatUsersCalls)
        assertEquals(AuthState.Authenticated, viewModel.authState.value)
    }

    @Test
    fun activeSession_staysInitializingUntilBothApisComplete() = runTest {
        val testSession = Session(
            token = "jwt-token",
            workspaceId = "workspace-1",
        )
        sessionFlow.value = testSession

        var completeProfileCall: (() -> Unit)? = null
        var completeChatUsersCall: (() -> Unit)? = null
        val deferredUserRepository = object : UserRepository {
            override suspend fun getProfile(): Result<Unit> {
                suspendCancellableCoroutine<Unit> { cont ->
                    completeProfileCall = {
                        cont.resume(Unit)
                    }
                }
                return Result.success(Unit)
            }

            override suspend fun getChatUsers(workspaceId: String?): Result<Unit> {
                suspendCancellableCoroutine<Unit> { cont ->
                    completeChatUsersCall = {
                        cont.resume(Unit)
                    }
                }
                return Result.success(Unit)
            }

            override suspend fun updateAvatar(uri: Uri): Result<User> = throw NotImplementedError()
        }

        val viewModel = AppViewModel(authRepository, deferredUserRepository, deepLinkParser, networkMonitor)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.authState.collect {}
        }

        // Before any completes -> state is still Initializing
        assertEquals(AuthState.Initializing, viewModel.authState.value)

        // After getProfile completes, but chat users still pending -> state is still Initializing
        completeProfileCall?.invoke()
        assertEquals(AuthState.Initializing, viewModel.authState.value)

        // After both complete -> state transitions to Authenticated
        completeChatUsersCall?.invoke()
        assertEquals(AuthState.Authenticated, viewModel.authState.value)
    }

    @Test
    fun unauthenticated_doesNotFetchUserProfileOrChatUsers() = runTest {
        sessionFlow.value = null

        val viewModel = AppViewModel(authRepository, userRepository, deepLinkParser, networkMonitor)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.authState.collect {}
        }

        assertEquals(0, userRepository.getProfileCalls)
        assertEquals(0, userRepository.getChatUsersCalls)
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)
    }

    @Test
    fun fetchErrors_areCaughtGracefully() = runTest {
        userRepository.shouldFail = true
        sessionFlow.value = Session(token = "jwt-token", workspaceId = "ws-1")

        val viewModel = AppViewModel(authRepository, userRepository, deepLinkParser, networkMonitor)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.authState.collect {}
        }

        assertEquals(1, userRepository.getProfileCalls)
        assertEquals(1, userRepository.getChatUsersCalls)
        assertEquals(AuthState.Authenticated, viewModel.authState.value)
    }

    @Test
    fun firstTimeSignIn_fetchesUserProfileAndChatUsersBeforeTransitioningToAuthenticated() = runTest {
        sessionFlow.value = null

        val viewModel = AppViewModel(authRepository, userRepository, deepLinkParser, networkMonitor)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.authState.collect {}
        }

        assertEquals(0, userRepository.getProfileCalls)
        assertEquals(0, userRepository.getChatUsersCalls)
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)

        // User signs in with single workspace
        sessionFlow.value = Session(token = "jwt-token", workspaceId = "ws-1")

        assertEquals(1, userRepository.getProfileCalls)
        assertEquals(1, userRepository.getChatUsersCalls)
        assertEquals(AuthState.Authenticated, viewModel.authState.value)
    }

    @Test
    fun selectWorkspace_fetchesUserProfileAndChatUsersBeforeTransitioningToAuthenticated() = runTest {
        sessionFlow.value = Session(token = "jwt-token", workspaceId = null)

        val viewModel = AppViewModel(authRepository, userRepository, deepLinkParser, networkMonitor)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.authState.collect {}
        }

        assertEquals(0, userRepository.getProfileCalls)
        assertEquals(0, userRepository.getChatUsersCalls)
        assertEquals(AuthState.SelectWorkspace, viewModel.authState.value)

        // User selects a workspace
        sessionFlow.value = Session(token = "jwt-token", workspaceId = "ws-2")

        assertEquals(1, userRepository.getProfileCalls)
        assertEquals(1, userRepository.getChatUsersCalls)
        assertEquals(AuthState.Authenticated, viewModel.authState.value)
    }

    @Test
    fun handleIntent_profileShortcut_setsPendingDestination() = runTest {
        val viewModel = AppViewModel(authRepository, userRepository, deepLinkParser, networkMonitor)
        val intent = mock(Intent::class.java)
        `when`(intent.getStringExtra(AppShortcutManager.EXTRA_SHORTCUT_ACTION)).thenReturn(AppShortcutManager.ACTION_OPEN_PROFILE)

        viewModel.handleIntent(intent)

        assertEquals(DeepLinkDestination.Profile, viewModel.pendingDestination.value)
    }

    @Test
    fun handleIntent_recentChannelShortcut_setsPendingDestination() = runTest {
        val viewModel = AppViewModel(authRepository, userRepository, deepLinkParser, networkMonitor)
        val intent = mock(Intent::class.java)
        `when`(intent.getStringExtra(AppShortcutManager.EXTRA_SHORTCUT_ACTION)).thenReturn(AppShortcutManager.ACTION_OPEN_CHANNEL)
        `when`(intent.getStringExtra(AppShortcutManager.EXTRA_CHANNEL_ID)).thenReturn("channel-123")

        viewModel.handleIntent(intent)

        assertEquals(DeepLinkDestination.Channel("channel-123"), viewModel.pendingDestination.value)
    }

    @Test
    fun isOffline_tracksNetworkStatus() = runTest {
        val viewModel = AppViewModel(authRepository, userRepository, deepLinkParser, networkMonitor)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.isOffline.collect {}
        }

        assertFalse(viewModel.isOffline.value)

        networkMonitor.isOnlineFlow.value = false
        assertTrue(viewModel.isOffline.value)

        networkMonitor.isOnlineFlow.value = true
        assertFalse(viewModel.isOffline.value)
    }

    private class FakeAuthRepository(override val session: Flow<Session?>) : AuthRepository {
        override suspend fun signIn(email: String, password: String): Result<SignInStatus> = throw NotImplementedError()
        override suspend fun signOut() {}
    }

    private class FakeUserRepository : UserRepository {
        var getProfileCalls = 0
        var getChatUsersCalls = 0
        var shouldFail = false

        override suspend fun getProfile(): Result<Unit> {
            getProfileCalls++
            if (shouldFail) return Result.failure(RuntimeException("Network error"))
            return Result.success(Unit)
        }

        override suspend fun getChatUsers(workspaceId: String?): Result<Unit> {
            getChatUsersCalls++
            if (shouldFail) return Result.failure(RuntimeException("Network error"))
            return Result.success(Unit)
        }

        override suspend fun updateAvatar(uri: Uri): Result<User> = throw NotImplementedError()
    }

    private class FakeNetworkMonitor : NetworkMonitor {
        val isOnlineFlow = MutableStateFlow(true)
        override val isOnline: Flow<Boolean> = isOnlineFlow
    }
}
