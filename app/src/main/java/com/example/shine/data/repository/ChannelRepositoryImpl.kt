package com.example.shine.data.repository

import com.example.shine.core.util.suspendRunCatching
import com.example.shine.data.local.ChannelDataStore
import com.example.shine.data.remote.ChatApi
import com.example.shine.data.remote.dto.CreateChannelRequestDto
import com.example.shine.data.remote.dto.toDomain
import com.example.shine.domain.model.Channel
import com.example.shine.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val channelDataStore: ChannelDataStore,
) : ChannelRepository {

    override suspend fun getChannels(): Result<List<Channel>> = suspendRunCatching {
        try {
            val channels = api.getChannels()
                .sortedBy { it.position ?: Long.MAX_VALUE }
                .map { it.toDomain() }
            channelDataStore.replaceChannels(channels)
            channels
        } catch (e: Exception) {
            channelDataStore.getChannels().ifEmpty { throw e }
        }
    }

    override suspend fun getChannel(id: String): Result<Channel> = suspendRunCatching {
        try {
            val channel = api.getConversation(id).toDomain()
            channelDataStore.updateChannel(channel)
            channel
        } catch (e: Exception) {
            channelDataStore.getChannel(id) ?: throw e
        }
    }

    override suspend fun createChannel(name: String): Result<Unit> = suspendRunCatching {
        val request = CreateChannelRequestDto(
            name = name,
            isPrivate = false,
            isEncrypted = false,
        )
        val channel = api.createChannel(request).toDomain()
        channelDataStore.updateChannel(channel)
    }

    override fun getChannelsFlow(): Flow<List<Channel>> = channelDataStore.channelsFlow

    override fun getChannelFlow(id: String): Flow<Channel?> = channelDataStore.getChannelFlow(id)

    override fun clear() {
        channelDataStore.clear()
    }
}
