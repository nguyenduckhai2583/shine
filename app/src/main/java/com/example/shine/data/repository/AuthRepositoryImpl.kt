package com.example.shine.data.repository

import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.remote.ApiErrorParser
import com.example.shine.data.remote.AuthApi
import com.example.shine.data.remote.dto.SignInRequest
import com.example.shine.data.remote.dto.toDomain
import com.example.shine.data.security.PasswordHasher
import com.example.shine.domain.model.AuthException
import com.example.shine.domain.model.Session
import com.example.shine.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val local: SessionLocalDataSource,
    private val passwordHasher: PasswordHasher,
    private val errorParser: ApiErrorParser,
) : AuthRepository {

    override val session: Flow<Session?> = local.session

    override suspend fun signIn(email: String, password: String): Result<Session> {
        val dto = try {
            api.signIn(SignInRequest(email = email, password = passwordHasher.sha1(password))).data
                ?: return Result.failure(AuthException.Unknown("Empty response"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(errorParser.toAuthException(e))
        }

        if (dto.isTmpToken == true) return Result.failure(AuthException.TwoFactorRequired())

        val session = dto.toDomain()
        local.save(session)
        return Result.success(session)
    }

    override suspend fun signOut() = local.clear()
}
