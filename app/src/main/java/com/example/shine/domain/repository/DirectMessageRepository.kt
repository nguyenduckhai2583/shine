package com.example.shine.domain.repository

import com.example.shine.domain.model.DirectMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface DirectMessageRepository {
    suspend fun getDirectMessages(): Result<List<DirectMessage>>

    fun getDirectMessagesFlow(): Flow<List<DirectMessage>> = emptyFlow()

    fun clear() {}
}
