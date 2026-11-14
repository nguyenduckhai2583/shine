package com.example.shine.core.data.repository

import com.example.shine.core.model.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface ChannelRepository {
    suspend fun getChannels(): Result<Unit>

    suspend fun getRecentChannels(limit: Int = 3): List<Channel> = emptyList()

    suspend fun getChannel(id: String): Result<Unit>

    suspend fun createChannel(name: String): Result<Unit>

    suspend fun deleteChannel(channelId: String): Result<Unit> = Result.success(Unit)

    fun getChannelsFlow(): Flow<List<Channel>> = getChannelsFlow(limit = -1)

    fun getChannelsFlow(limit: Int): Flow<List<Channel>> = emptyFlow()

    fun getChannelFlow(id: String): Flow<Channel?> = emptyFlow()

    fun onChannelCreated(channel: Channel) {}

    suspend fun clear() {}
}
