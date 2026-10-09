package com.example.shine.domain.repository

import com.example.shine.domain.model.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface ChannelRepository {
    suspend fun getChannels(): List<Channel>

    suspend fun getChannel(id: String): Channel

    fun getChannelsFlow(): Flow<List<Channel>> = emptyFlow()

    fun getChannelFlow(id: String): Flow<Channel?> = emptyFlow()

    fun clear() {}
}
