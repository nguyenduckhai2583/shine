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
    private val workspaceRepository: WorkspaceRepository,
    private val channelRepository: ChannelRepository,
    private val directMessageRepository: DirectMessageRepository,
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AuthRepository {

    override val session: Flow<Session?> = local.session

    override suspend fun signIn(email: String, password: String): Result<SignInStatus> = suspendRunCatching {
        val dto = api.signIn(SignInRequest(email = email, password = passwordHasher.sha1(password)))

        if (dto.isTmpToken == true) {
            return@suspendRunCatching SignInStatus.TwoFactorRequired(tmpToken = dto.token)
        }

        channelRepository.clear()
        workspaceRepository.clear()
        directMessageRepository.clear()
        userRepository.clear()

        val session = dto.toDomain()
        local.save(session)

        val workspaces = workspaceRepository.fetchWorkspaces().getOrElse { emptyList() }

        if (workspaces.size == 1) {
            val single = workspaces.first()
            local.setWorkspaceId(single.id)
            return@suspendRunCatching SignInStatus.Authenticated(session.copy(workspaceId = single.id))
        }

        SignInStatus.Authenticated(session)
    }

    override suspend fun signOut() {
        withContext(NonCancellable + ioDispatcher) {
            channelRepository.clear()
            workspaceRepository.clear()
            directMessageRepository.clear()
            userRepository.clear()
            local.clear()
        }
    }
}
