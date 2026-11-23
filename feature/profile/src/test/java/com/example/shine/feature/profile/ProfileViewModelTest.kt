package com.example.shine.feature.profile

import android.net.Uri
import app.cash.turbine.test
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.data.repository.AuthRepository
import com.example.shine.core.data.repository.UserRepository
import com.example.shine.core.model.Session
import com.example.shine.core.model.SignInStatus
import com.example.shine.core.model.User
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
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Jane Doe", state.name)
            assertEquals("https://example.com/avatar.jpg", state.avatarUrl)
            assertFalse(state.isLoading)
            assertEquals(UploadState.Idle, state.uploadState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onAvatarSelected_success_updatesAvatarAndClearsUploading() = runTest {
        val viewModel = ProfileViewModel(authRepository, userRepository)
        viewModel.uiState.test {
            awaitItem() // initial state
            val uri = mock(Uri::class.java)
            viewModel.onAvatarSelected(uri)

            val uploadingState = awaitItem()
            assertEquals(UploadState.Uploading(uri), uploadingState.uploadState)

            val idleState = awaitItem()
            assertEquals(UploadState.Idle, idleState.uploadState)
            assertEquals(uri, userRepository.lastUpdatedUri)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onAvatarSelected_failure_setsErrorMessageAndClearsUploading() = runTest {
        userRepository.shouldFail = true
        val viewModel = ProfileViewModel(authRepository, userRepository)
        viewModel.uiState.test {
            awaitItem() // initial state
            val uri = mock(Uri::class.java)
            viewModel.onAvatarSelected(uri)

            val uploadingState = awaitItem()
            assertEquals(UploadState.Uploading(uri), uploadingState.uploadState)

            val errorState = awaitItem()
            assertEquals(UploadState.Error(UiText.Raw("Network upload failed")), errorState.uploadState)

            viewModel.clearErrorMessage()
            val idleState = awaitItem()
            assertEquals(UploadState.Idle, idleState.uploadState)
            cancelAndIgnoreRemainingEvents()
        }
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

        override suspend fun getProfile(): Result<Unit> {
            if (shouldFail) {
                return Result.failure(RuntimeException("Network request failed"))
            }
            return Result.success(Unit)
        }

        override suspend fun getChatUsers(workspaceId: String?): Result<Unit> = Result.success(Unit)

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

        override suspend fun clear() {}
    }
}
