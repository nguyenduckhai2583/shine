package com.example.shine.ui.navigation

import android.net.Uri
import com.example.shine.domain.model.Session
import com.example.shine.domain.model.User
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.domain.repository.UserRepository
import com.example.shine.ui.navigation.deeplink.DeepLinkParser
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val sessionFlow = MutableStateFlow<Session?>(null)
    private val authRepository = FakeAuthRepository(sessionFlow)
    private val userRepository = FakeUserRepository()
    private val deepLinkParser = DeepLinkParser()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun activeSession_fetchesUserProfileOnLaunch() = runTest {
        val testSession = Session(
            token = "jwt-token",
            workspaceId = "workspace-1",
            user = User(id = "user-1", email = "user@test.com", firstName = "Jane", lastName = "Doe"),
        )
        sessionFlow.value = testSession

        val viewModel = AppViewModel(authRepository, userRepository, deepLinkParser)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.authState.collect {}
        }

        assertEquals(1, userRepository.getProfileCalls)
        assertEquals(AuthState.SignedIn, viewModel.authState.value)
    }

    @Test
    fun signedOut_doesNotFetchUserProfile() = runTest {
        sessionFlow.value = null

        val viewModel = AppViewModel(authRepository, userRepository, deepLinkParser)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.authState.collect {}
        }

        assertEquals(0, userRepository.getProfileCalls)
        assertEquals(AuthState.SignedOut, viewModel.authState.value)
    }

    @Test
    fun profileFetchError_isCaughtGracefully() = runTest {
        userRepository.shouldFail = true
        sessionFlow.value = Session(token = "jwt-token", workspaceId = "ws-1")

        val viewModel = AppViewModel(authRepository, userRepository, deepLinkParser)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.authState.collect {}
        }

        assertEquals(1, userRepository.getProfileCalls)
        assertEquals(AuthState.SignedIn, viewModel.authState.value)
    }

    private class FakeAuthRepository(override val session: Flow<Session?>) : AuthRepository {
        override suspend fun signIn(email: String, password: String): Session = throw NotImplementedError()
        override suspend fun signOut() {}
    }

    private class FakeUserRepository : UserRepository {
        var getProfileCalls = 0
        var shouldFail = false

        override suspend fun getProfile(): User {
            getProfileCalls++
            if (shouldFail) throw RuntimeException("Network error")
            return User(id = "user-1", email = "user@test.com", firstName = "Jane", lastName = "Doe")
        }

        override suspend fun updateAvatar(uri: Uri): User = throw NotImplementedError()
    }
}
