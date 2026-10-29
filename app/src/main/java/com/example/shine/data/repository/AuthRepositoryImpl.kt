package com.example.shine.data.repository

import com.example.shine.core.util.suspendRunCatching
import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.remote.AuthApi
import com.example.shine.data.remote.dto.SignInRequest
import com.example.shine.data.remote.dto.toDomain
import com.example.shine.data.security.PasswordHasher
import com.example.shine.domain.model.Session
import com.example.shine.domain.model.SignInStatus
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.domain.repository.ChannelRepository
import com.example.shine.domain.repository.WorkspaceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val local: SessionLocalDataSource,
    private val workspaceRepository: WorkspaceRepository,
    private val channelRepository: ChannelRepository,
    private val passwordHasher: PasswordHasher,
) : AuthRepository {

    override val session: Flow<Session?> = local.session

    override suspend fun signIn(email: String, password: String): Result<SignInStatus> = suspendRunCatching {
        val dto = api.signIn(SignInRequest(email = email, password = passwordHasher.sha1(password)))

        if (dto.isTmpToken == true) {
            return@suspendRunCatching SignInStatus.TwoFactorRequired(tmpToken = dto.token.orEmpty())
        }

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
        local.clear()
        workspaceRepository.clear()
        channelRepository.clear()
    }
}
