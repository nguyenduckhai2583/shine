package com.example.shine.core.data.repository

import com.example.shine.core.model.Channel
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    suspend fun getChannels(): Result<Unit>

    suspend fun getRecentChannels(limit: Int = 3): List<Channel>

    suspend fun getChannel(id: String): Result<Unit>

    suspend fun createChannel(name: String): Result<Unit>

    suspend fun deleteChannel(channelId: String): Result<Unit>

    fun getChannelsFlow(limit: Int = -1): Flow<List<Channel>>

    fun getChannelFlow(id: String): Flow<Channel?>

    suspend fun onChannelCreated(channel: Channel)

    suspend fun clear()
}
