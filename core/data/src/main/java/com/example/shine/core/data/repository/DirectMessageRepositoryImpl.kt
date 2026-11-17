package com.example.shine.core.data.repository

import com.example.shine.core.common.util.suspendRunCatching
import com.example.shine.core.data.source.local.UserLocalDataSource
import com.example.shine.core.database.dao.DirectMessageDao
import com.example.shine.core.database.entity.toDomain
import com.example.shine.core.database.entity.toEntity
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.model.DirectMessage
import com.example.shine.core.network.ChatApi
import com.example.shine.core.network.dto.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectMessageRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val userLocalDataSource: UserLocalDataSource,
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val directMessageDao: DirectMessageDao,
) : DirectMessageRepository {

    override suspend fun getDirectMessages(): Result<Unit> = suspendRunCatching {
        val currentUserId = sessionLocalDataSource.session.firstOrNull()?.user?.id
        val chatUsersMap = userLocalDataSource.getUsers().associate { user ->
            val name = listOfNotNull(user.firstName, user.lastName)
                .joinToString(" ")
                .trim()
                .ifEmpty { user.email.orEmpty() }
            user.id to name
        }

        val rawDms = chatApi.getDirectMessages()
        val dms = rawDms.map { it.toDomain(currentUserId, chatUsersMap) }
            .sortedWith { a, b ->
                val t1 = a.time ?: 0L
                val t2 = b.time ?: 0L
                t2.compareTo(t1)
            }

        directMessageDao.replaceDirectMessages(dms.map { it.toEntity() })
    }

    override suspend fun getDirectMessage(id: String): Result<Unit> = suspendRunCatching {
        getDirectMessages().getOrThrow()
    }

    override fun getDirectMessagesFlow(): Flow<List<DirectMessage>> =
        directMessageDao.observeDirectMessages().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getDirectMessageFlow(id: String): Flow<DirectMessage?> =
        directMessageDao.observeDirectMessage(id).map { it?.toDomain() }

    override suspend fun onDirectMessageCreated(message: DirectMessage) {
        directMessageDao.upsertDirectMessage(message.toEntity())
    }

    override suspend fun clear() {
        directMessageDao.clearAll()
    }
}
