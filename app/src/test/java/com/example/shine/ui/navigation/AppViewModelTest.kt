package com.example.shine.ui.navigation

import android.content.Intent
import android.net.Uri
import app.cash.turbine.test
import com.example.shine.core.common.network.NetworkMonitor
import com.example.shine.core.data.repository.UserRepository
import com.example.shine.core.data.session.SessionPreloader
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.model.AuthState
import com.example.shine.core.model.Session
import com.example.shine.core.model.User
import com.example.shine.ui.navigation.deeplink.AppDeepLinkHandler
import kotlinx.coroutines.CoroutineScope
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val sessionFlow = MutableStateFlow<Session?>(null)
    private lateinit var sessionLocal: SessionLocalDataSource
    private lateinit var sessionManager: SessionManager
    private val userRepository = FakeUserRepository()
    private lateinit var sessionPreloader: SessionPreloader
    private val deepLinkHandler = mock(AppDeepLinkHandler::class.java)
    private val networkMonitor = FakeNetworkMonitor()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = CoroutineScope(testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionLocal = mock(SessionLocalDataSource::class.java)
        `when`(sessionLocal.session).thenReturn(sessionFlow)
        sessionManager = SessionManager(sessionLocal, testScope)
        sessionPreloader = SessionPreloader(userRepository)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun activeSession_fetchesUserProfileAndChatUsersOnLaunch() = runTest(testDispatcher) {
        val testSession = Session(
            token = "jwt-token",
            workspaceId = "workspace-1",
            user = User(id = "user-1", email = "user@test.com", firstName = "Jane", lastName = "Doe"),
        )
        sessionFlow.value = testSession

        val viewModel = AppViewModel(sessionManager, sessionPreloader, deepLinkHandler, networkMonitor)

        viewModel.authState.test {
            assertEquals(AuthState.Authenticated, awaitItem())
            assertEquals(1, userRepository.getProfileCalls)
            assertEquals(1, userRepository.getChatUsersCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unauthenticated_doesNotFetchUserProfileOrChatUsers() = runTest(testDispatcher) {
        sessionFlow.value = null

        val viewModel = AppViewModel(sessionManager, sessionPreloader, deepLinkHandler, networkMonitor)

        viewModel.authState.test {
            assertEquals(AuthState.Unauthenticated, awaitItem())
            assertEquals(0, userRepository.getProfileCalls)
            assertEquals(0, userRepository.getChatUsersCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun fetchErrors_areCaughtGracefully() = runTest(testDispatcher) {
        userRepository.shouldFail = true
        sessionFlow.value = Session(token = "jwt-token", workspaceId = "ws-1")

        val viewModel = AppViewModel(sessionManager, sessionPreloader, deepLinkHandler, networkMonitor)

        viewModel.authState.test {
            assertEquals(AuthState.Authenticated, awaitItem())
            assertEquals(1, userRepository.getProfileCalls)
            assertEquals(1, userRepository.getChatUsersCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun firstTimeSignIn_fetchesUserProfileAndChatUsers() = runTest(testDispatcher) {
        sessionFlow.value = null

        val viewModel = AppViewModel(sessionManager, sessionPreloader, deepLinkHandler, networkMonitor)

        viewModel.authState.test {
            assertEquals(AuthState.Unauthenticated, awaitItem())
            assertEquals(0, userRepository.getProfileCalls)
            assertEquals(0, userRepository.getChatUsersCalls)

            // User signs in with single workspace
            sessionFlow.value = Session(token = "jwt-token", workspaceId = "ws-1")

            assertEquals(AuthState.Authenticated, awaitItem())
            assertEquals(1, userRepository.getProfileCalls)
            assertEquals(1, userRepository.getChatUsersCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectWorkspace_fetchesUserProfileAndChatUsersWhenWorkspaceSelected() = runTest(testDispatcher) {
        sessionFlow.value = Session(token = "jwt-token", workspaceId = null)

        val viewModel = AppViewModel(sessionManager, sessionPreloader, deepLinkHandler, networkMonitor)

        viewModel.authState.test {
            assertEquals(AuthState.SelectWorkspace, awaitItem())
            assertEquals(0, userRepository.getProfileCalls)
            assertEquals(0, userRepository.getChatUsersCalls)

            // User selects a workspace
            sessionFlow.value = Session(token = "jwt-token", workspaceId = "ws-2")

            assertEquals(AuthState.Authenticated, awaitItem())
            assertEquals(1, userRepository.getProfileCalls)
            assertEquals(1, userRepository.getChatUsersCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectWorkspace_doesNotResetSessionPreloader_butLogoutDoes() = runTest(testDispatcher) {
        sessionFlow.value = Session(token = "jwt-token", workspaceId = "ws-1")

        val viewModel = AppViewModel(sessionManager, sessionPreloader, deepLinkHandler, networkMonitor)

        viewModel.authState.test {
            assertEquals(AuthState.Authenticated, awaitItem())
            assertEquals(1, userRepository.getProfileCalls)

            // Switching to select workspace does not reset session preloader
            sessionFlow.value = Session(token = "jwt-token", workspaceId = null)
            assertEquals(AuthState.SelectWorkspace, awaitItem())

            sessionFlow.value = Session(token = "jwt-token", workspaceId = "ws-2")
            assertEquals(AuthState.Authenticated, awaitItem())
            assertEquals(1, userRepository.getProfileCalls) // Still 1, not refetched

            // Logout resets session preloader
            sessionFlow.value = null
            assertEquals(AuthState.Unauthenticated, awaitItem())

            // Re-login fetches again
            sessionFlow.value = Session(token = "new-jwt-token", workspaceId = "ws-1")
            assertEquals(AuthState.Authenticated, awaitItem())
            assertEquals(2, userRepository.getProfileCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun handleIntent_profileShortcut_setsDeepLinkBackStack() = runTest(testDispatcher) {
        sessionFlow.value = Session(token = "jwt-token", workspaceId = "ws-1")
        val viewModel = AppViewModel(sessionManager, sessionPreloader, deepLinkHandler, networkMonitor)
        val intent = mock(Intent::class.java)
        `when`(deepLinkHandler.handleIntent(intent)).thenReturn(listOf(Home, Profile))

        viewModel.handleIntent(intent)

        assertEquals(listOf(Home, Profile), viewModel.deepLinkBackStack.value)
    }

    @Test
    fun handleIntent_nullIntent_doesNotSetDeepLinkBackStack() = runTest(testDispatcher) {
        sessionFlow.value = Session(token = "jwt-token", workspaceId = "ws-1")
        val viewModel = AppViewModel(sessionManager, sessionPreloader, deepLinkHandler, networkMonitor)

        viewModel.handleIntent(null)

        assertNull(viewModel.deepLinkBackStack.value)
    }

    @Test
    fun handleIntent_unhandledIntent_doesNotSetDeepLinkBackStack() = runTest(testDispatcher) {
        sessionFlow.value = Session(token = "jwt-token", workspaceId = "ws-1")
        val viewModel = AppViewModel(sessionManager, sessionPreloader, deepLinkHandler, networkMonitor)
        val intent = mock(Intent::class.java)
        `when`(deepLinkHandler.handleIntent(intent)).thenReturn(null)

        viewModel.handleIntent(intent)

        assertNull(viewModel.deepLinkBackStack.value)
    }

    @Test
    fun handleIntent_webUriChannel_setsChannelDetailBackStack() = runTest(testDispatcher) {
        sessionFlow.value = Session(token = "jwt-token", workspaceId = "ws-1")
        val viewModel = AppViewModel(sessionManager, sessionPreloader, deepLinkHandler, networkMonitor)
        val uri = mock(Uri::class.java)
        val intent = mock(Intent::class.java).apply {
            `when`(data).thenReturn(uri)
        }
        `when`(deepLinkHandler.handleIntent(intent)).thenReturn(listOf(Home, ChannelDetail("c1")))

        viewModel.handleIntent(intent)

        assertEquals(listOf(Home, ChannelDetail("c1")), viewModel.deepLinkBackStack.value)
    }

    @Test
    fun onDeepLinkConsumed_clearsDeepLinkBackStack() = runTest(testDispatcher) {
        sessionFlow.value = Session(token = "jwt-token", workspaceId = "ws-1")
        val viewModel = AppViewModel(sessionManager, sessionPreloader, deepLinkHandler, networkMonitor)
        val intent = mock(Intent::class.java)
        `when`(deepLinkHandler.handleIntent(intent)).thenReturn(listOf(Home, ChannelDetail("general")))

        viewModel.handleIntent(intent)
        assertEquals(listOf(Home, ChannelDetail("general")), viewModel.deepLinkBackStack.value)

        viewModel.onDeepLinkConsumed()
        assertNull(viewModel.deepLinkBackStack.value)
    }

    @Test
    fun isOffline_tracksNetworkStatus() = runTest(testDispatcher) {
        val viewModel = AppViewModel(sessionManager, sessionPreloader, deepLinkHandler, networkMonitor)
        viewModel.isOffline.test {
            assertFalse(awaitItem())

            networkMonitor.isOnlineFlow.value = false
            assertTrue(awaitItem())

            networkMonitor.isOnlineFlow.value = true
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
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
        override suspend fun clear() {}
    }

    private class FakeNetworkMonitor : NetworkMonitor {
        val isOnlineFlow = MutableStateFlow(true)
        override val isOnline: Flow<Boolean> = isOnlineFlow
    }
}
