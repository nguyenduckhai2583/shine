package com.example.shine.core.data.repository

import com.example.shine.core.model.DirectMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface DirectMessageRepository {
    suspend fun getDirectMessages(): Result<Unit>

    fun getDirectMessagesFlow(): Flow<List<DirectMessage>> = emptyFlow()

    fun onDirectMessageCreated(message: DirectMessage) {}

    fun clear() {}
}
