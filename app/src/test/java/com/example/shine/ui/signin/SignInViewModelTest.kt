package com.example.shine.ui.signin

import com.example.shine.domain.model.AuthException
import com.example.shine.domain.model.Session
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.domain.usecase.SignInUseCase
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

    private val repository = FakeAuthRepository()
    private lateinit var viewModel: SignInViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = SignInViewModel(SignInUseCase(repository))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

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
        repository.nextResult = Result.failure(AuthException.Server("Wrong password"))

        viewModel.signIn("a@b.com")

        assertEquals("Wrong password", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun editingInput_clearsError() = runTest {
        repository.nextResult = Result.failure(AuthException.Network())
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

private class FakeAuthRepository : AuthRepository {
    override val session = MutableStateFlow<Session?>(null)
    var nextResult: Result<Session>? = null
    var lastEmail: String? = null

    override suspend fun signIn(email: String, password: String): Result<Session> {
        lastEmail = email
        val result = nextResult ?: Result.success(Session("token", null, null, null))
        result.onSuccess { session.value = it }
        return result
    }

    override suspend fun signOut() {
        session.value = null
    }
}
