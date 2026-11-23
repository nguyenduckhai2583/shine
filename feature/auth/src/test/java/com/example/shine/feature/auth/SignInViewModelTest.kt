package com.example.shine.feature.auth

import com.example.shine.core.common.error.AppErrorEnum
import com.example.shine.core.common.error.AppException
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.testing.repository.FakeAuthRepository
import com.example.shine.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SignInViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private lateinit var viewModel: SignInViewModel

    @Before
    fun setUp() {
        viewModel = SignInViewModel(repository)
    }

    @Test
    fun signIn_success_persistsSessionAndClearsError() = runTest {
        viewModel.onEmailChange(" a@b.com ")
        viewModel.onPasswordChange("secret")

        viewModel.signIn("a@b.com")

        assertEquals("a@b.com", repository.lastEmail)
        assertEquals("token", repository.session.value?.token)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun signIn_failure_showsServerMessage() = runTest {
        repository.nextError = AppException(AppErrorEnum.SERVER, errorMessage = "Wrong password")

        viewModel.signIn("a@b.com")

        assertEquals(UiText.Raw("Wrong password"), viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun editingInput_clearsError() = runTest {
        repository.nextError = AppException(AppErrorEnum.NO_INTERNET)
        viewModel.signIn("a@b.com")

        viewModel.onPasswordChange("other")

        assertNull(viewModel.uiState.value.errorMessage)
    }

    private fun SignInViewModel.signIn(email: String) {
        onEmailChange(email)
        if (uiState.value.password.isEmpty()) onPasswordChange("secret")
        signIn()
    }
}
