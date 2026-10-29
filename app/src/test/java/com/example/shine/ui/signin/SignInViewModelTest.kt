package com.example.shine.ui.signin

import com.example.shine.domain.model.AppErrorEnum
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Session
import com.example.shine.domain.model.SignInStatus
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.ui.common.UiText
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
        viewModel = SignInViewModel(repository)
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

private class FakeAuthRepository : AuthRepository {
    override val session = MutableStateFlow<Session?>(null)
    var nextError: AppException? = null
    var lastEmail: String? = null

    override suspend fun signIn(email: String, password: String): Result<SignInStatus> {
        lastEmail = email
        val err = nextError
        if (err != null) return Result.failure(err)
        val sessionObj = Session("token", null, null, null).also { session.value = it }
        return Result.success(SignInStatus.Authenticated(sessionObj))
    }

    override suspend fun signOut() {
        session.value = null
    }
}
