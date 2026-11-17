package com.example.shine.core.data.repository

import com.example.shine.core.model.DirectMessage
import kotlinx.coroutines.flow.Flow

interface DirectMessageRepository {
    suspend fun getDirectMessages(): Result<Unit>

    suspend fun getDirectMessage(id: String): Result<Unit>

    fun getDirectMessagesFlow(): Flow<List<DirectMessage>>

    fun getDirectMessageFlow(id: String): Flow<DirectMessage?>

    suspend fun onDirectMessageCreated(message: DirectMessage)

    suspend fun clear()
}
