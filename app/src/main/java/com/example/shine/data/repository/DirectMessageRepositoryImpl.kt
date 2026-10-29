package com.example.shine.data.repository

import com.example.shine.core.util.suspendRunCatching
import com.example.shine.data.local.DirectMessageDataStore
import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.remote.ChatApi
import com.example.shine.data.remote.UserApi
import com.example.shine.data.remote.dto.toDomain
import com.example.shine.domain.model.DirectMessage
import com.example.shine.domain.repository.DirectMessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectMessageRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val userApi: UserApi,
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val directMessageDataStore: DirectMessageDataStore,
) : DirectMessageRepository {

    override suspend fun getDirectMessages(): Result<List<DirectMessage>> = suspendRunCatching {
        try {
            val currentUserId = sessionLocalDataSource.session.firstOrNull()?.user?.id
            val chatUsersMap = runCatching {
                userApi.getChatUsers().associate { userDto ->
                    val name = listOfNotNull(userDto.firstName, userDto.lastName)
                        .joinToString(" ")
                        .trim()
                        .ifEmpty { userDto.email.orEmpty() }
                    userDto.id to name
                }
            }.getOrDefault(emptyMap())

            val rawDms = chatApi.getDirectMessages()
            val dms = rawDms.map { it.toDomain(currentUserId, chatUsersMap) }
                .sortedWith { a, b ->
                    val t1 = a.time ?: 0L
                    val t2 = b.time ?: 0L
                    t2.compareTo(t1)
                }

            directMessageDataStore.replaceDirectMessages(dms)
            dms
        } catch (e: Exception) {
            directMessageDataStore.getDirectMessages().ifEmpty { throw e }
        }
    }

    override fun getDirectMessagesFlow(): Flow<List<DirectMessage>> =
        directMessageDataStore.directMessagesFlow

    override fun clear() {
        directMessageDataStore.clear()
    }
}
