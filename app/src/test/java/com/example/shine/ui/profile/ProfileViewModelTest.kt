package com.example.shine.ui.profile

import android.net.Uri
import com.example.shine.domain.model.Session
import com.example.shine.domain.model.SignInStatus
import com.example.shine.domain.model.User
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.domain.repository.UserRepository
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val authRepository = FakeAuthRepository()
    private val userRepository = FakeUserRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun init_observesUserSessionDisplayNameAndAvatar() = runTest {
        val viewModel = ProfileViewModel(authRepository, userRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertEquals("Jane Doe", state.name)
        assertEquals("https://example.com/avatar.jpg", state.avatarUrl)
        assertFalse(state.isLoading)
    }

    @Test
    fun onAvatarSelected_success_updatesAvatarAndClearsUploading() = runTest {
        val viewModel = ProfileViewModel(authRepository, userRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val uri = mock(Uri::class.java)
        viewModel.onAvatarSelected(uri)

        val state = viewModel.uiState.value
        assertFalse(state.isUploading)
        assertNull(state.selectedAvatarUri)
        assertNull(state.errorMessage)
        assertEquals(uri, userRepository.lastUpdatedUri)
    }

    @Test
    fun onAvatarSelected_failure_setsErrorMessageAndClearsUploading() = runTest {
        userRepository.shouldFail = true
        val viewModel = ProfileViewModel(authRepository, userRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        val uri = mock(Uri::class.java)
        viewModel.onAvatarSelected(uri)

        val state = viewModel.uiState.value
        assertFalse(state.isUploading)
        assertNull(state.selectedAvatarUri)
        assertEquals(UiText.Raw("Network upload failed"), state.errorMessage)

        viewModel.clearErrorMessage()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    private class FakeAuthRepository : AuthRepository {
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
        override suspend fun signOut() {}
    }

    private class FakeUserRepository : UserRepository {
        var lastUpdatedUri: Uri? = null
        var shouldFail: Boolean = false

        override suspend fun getProfile(): Result<User> {
            if (shouldFail) {
                return Result.failure(RuntimeException("Network request failed"))
            }
            return Result.success(
                User(
                    id = "1",
                    email = "jane@example.com",
                    firstName = "Jane",
                    lastName = "Doe",
                    avatarUrl = "https://example.com/avatar.jpg",
                )
            )
        }

        override suspend fun updateAvatar(uri: Uri): Result<User> {
            lastUpdatedUri = uri
            if (shouldFail) {
                return Result.failure(RuntimeException("Network upload failed"))
            }
            return Result.success(
                User(
                    id = "1",
                    email = "jane@example.com",
                    firstName = "Jane",
                    lastName = "Doe",
                    avatarUrl = "https://example.com/new_avatar.jpg",
                )
            )
        }
    }
}
