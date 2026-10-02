package com.example.shine.domain.repository

import com.example.shine.domain.model.Channel

interface ChannelRepository {
    suspend fun getChannels(): Result<List<Channel>>

    suspend fun getChannel(id: String): Result<Channel>
}
