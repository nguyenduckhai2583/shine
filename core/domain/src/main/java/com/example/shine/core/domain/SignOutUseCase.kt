package com.example.shine.core.domain

import com.example.shine.core.common.di.IoDispatcher
import com.example.shine.core.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface SignOutUseCase {
    suspend operator fun invoke()
}

class SignOutUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SignOutUseCase {
    override suspend operator fun invoke() = withContext(NonCancellable + ioDispatcher) {
        authRepository.signOut()
    }
}
