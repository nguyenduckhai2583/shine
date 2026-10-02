package com.example.shine.data.repository

import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.remote.AuthApi
import com.example.shine.data.remote.dto.SignInRequest
import com.example.shine.data.remote.dto.toDomain
import com.example.shine.data.security.PasswordHasher
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Session
import com.example.shine.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val local: SessionLocalDataSource,
    private val passwordHasher: PasswordHasher,
) : AuthRepository {

    override val session: Flow<Session?> = local.session

    override suspend fun signIn(email: String, password: String): Session {
        val dto = api.signIn(SignInRequest(email = email, password = passwordHasher.sha1(password)))

        if (dto.isTmpToken == true) throw AppException(AppError.TWO_FACTOR_REQUIRED)

        val session = dto.toDomain()
        local.save(session)
        return session
    }

    override suspend fun signOut() = local.clear()
}
