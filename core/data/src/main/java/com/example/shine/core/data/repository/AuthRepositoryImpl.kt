package com.example.shine.core.data.repository

import com.example.shine.core.common.di.IoDispatcher
import com.example.shine.core.common.util.suspendRunCatching
import com.example.shine.core.data.security.PasswordHasher
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.model.Session
import com.example.shine.core.model.SignInStatus
import com.example.shine.core.network.AuthApi
import com.example.shine.core.network.dto.SignInRequest
import com.example.shine.core.network.dto.toDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val local: SessionLocalDataSource,
    private val passwordHasher: PasswordHasher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AuthRepository {

    override val session: Flow<Session?> = local.session

    override suspend fun signIn(email: String, password: String): Result<SignInStatus> = suspendRunCatching {
        val dto = api.signIn(SignInRequest(email = email, password = passwordHasher.sha1(password)))

        if (dto.isTmpToken == true) {
            return@suspendRunCatching SignInStatus.TwoFactorRequired(tmpToken = dto.token)
        }

        val session = dto.toDomain()
        local.save(session)
        SignInStatus.Authenticated(session)
    }

    override suspend fun signOut() {
        withContext(NonCancellable + ioDispatcher) {
            local.clear()
        }
    }
}
