package com.example.shine.core.data.repository

import com.example.shine.core.model.Channel
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    suspend fun getChannels(workspaceId: String): Result<Unit>

    suspend fun getRecentChannels(workspaceId: String, limit: Int = 3): List<Channel>

    suspend fun getChannel(id: String, workspaceId: String): Result<Unit>

    suspend fun createChannel(name: String, workspaceId: String): Result<Unit>

    suspend fun deleteChannel(channelId: String): Result<Unit>

    fun getChannelsFlow(workspaceId: String, limit: Int = -1): Flow<List<Channel>>

    fun getChannelFlow(id: String): Flow<Channel?>

    suspend fun onChannelCreated(channel: Channel, workspaceId: String)

    suspend fun clear()
}
