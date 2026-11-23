package com.example.shine.core.domain

import com.example.shine.core.testing.repository.FakeAuthRepository
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
}
