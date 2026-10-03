package com.example.shine.data.repository

import com.example.shine.data.local.dao.ChannelDao
import com.example.shine.data.local.entity.toDomain
import com.example.shine.data.local.entity.toEntity
import com.example.shine.data.remote.ChatApi
import com.example.shine.domain.model.Channel
import com.example.shine.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val channelDao: ChannelDao,
) : ChannelRepository {

    override suspend fun getChannels(): List<Channel> {
        return try {
            val dtos = api.getChannels().sortedBy { it.position ?: Long.MAX_VALUE }
            val entities = dtos.map { it.toEntity() }
            channelDao.replaceChannels(entities)
            entities.map { it.toDomain() }
        } catch (e: Exception) {
            val cached = channelDao.getChannels()
            if (cached.isNotEmpty()) {
                cached.map { it.toDomain() }
            } else {
                throw e
            }
        }
    }

    override suspend fun getChannel(id: String): Channel {
        return try {
            val dto = api.getConversation(id)
            val entity = dto.toEntity()
            channelDao.insertChannel(entity)
            entity.toDomain()
        } catch (e: Exception) {
            channelDao.getChannel(id)?.toDomain() ?: throw e
        }
    }

    override fun getChannelsFlow(): Flow<List<Channel>> {
        return channelDao.getChannelsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getChannelFlow(id: String): Flow<Channel?> {
        return channelDao.getChannelFlow(id).map { it?.toDomain() }
    }
}
