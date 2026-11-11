package com.example.shine.core.data.repository

import com.example.shine.core.common.util.suspendRunCatching
import com.example.shine.core.data.source.local.UserLocalDataSource
import com.example.shine.core.datastore.DirectMessageDataStore
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.model.DirectMessage
import com.example.shine.core.network.ChatApi
import com.example.shine.core.network.dto.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectMessageRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val userLocalDataSource: UserLocalDataSource,
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val directMessageDataStore: DirectMessageDataStore,
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

        directMessageDataStore.replaceDirectMessages(dms)
    }

    override fun getDirectMessagesFlow(): Flow<List<DirectMessage>> =
        directMessageDataStore.directMessagesFlow

    override fun onDirectMessageCreated(message: DirectMessage) {
        directMessageDataStore.updateDirectMessage(message)
    }

    override fun clear() {
        directMessageDataStore.clear()
    }
}
