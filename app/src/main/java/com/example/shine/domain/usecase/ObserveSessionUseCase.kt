package com.example.shine.domain.usecase

import com.example.shine.domain.model.Session
import com.example.shine.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<Session?> = authRepository.session
}
