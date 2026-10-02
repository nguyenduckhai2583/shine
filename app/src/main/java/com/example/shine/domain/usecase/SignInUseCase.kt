package com.example.shine.domain.usecase

import com.example.shine.domain.model.Session
import com.example.shine.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<Session> =
        authRepository.signIn(email = email.trim(), password = password)
}
