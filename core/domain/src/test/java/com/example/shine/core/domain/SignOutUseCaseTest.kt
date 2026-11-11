package com.example.shine.core.domain

import com.example.shine.core.data.repository.AuthRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SignOutUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var useCase: SignOutUseCaseImpl

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
        useCase = SignOutUseCaseImpl(
            authRepository = authRepository,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun invoke_delegatesSignOutToAuthRepository() = runTest(testDispatcher) {
        useCase()
        assertTrue(authRepository.signOutCalled)
    }

    private class FakeAuthRepository : AuthRepository {
        var signOutCalled = false
        override val session = kotlinx.coroutines.flow.flowOf(null)
        override suspend fun signIn(email: String, password: String) =
            throw UnsupportedOperationException()
        override suspend fun signOut() {
            signOutCalled = true
        }
    }
}
