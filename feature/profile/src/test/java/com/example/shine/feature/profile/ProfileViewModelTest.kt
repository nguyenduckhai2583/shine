package com.example.shine.feature.profile

import android.net.Uri
import app.cash.turbine.test
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.model.Session
import com.example.shine.core.model.User
import com.example.shine.core.testing.repository.FakeAuthRepository
import com.example.shine.core.testing.repository.FakeUserRepository
import com.example.shine.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val initialSession = Session(
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
    private val authRepository = FakeAuthRepository(initialSession)
    private val userRepository = FakeUserRepository().apply {
        failureException = RuntimeException("Network upload failed")
    }

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
}
