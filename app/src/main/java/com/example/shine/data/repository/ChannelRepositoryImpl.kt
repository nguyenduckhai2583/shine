package com.example.shine.data.repository

import com.example.shine.core.util.suspendRunCatching
import com.example.shine.data.local.dao.ChannelDao
import com.example.shine.data.local.entity.ChannelEntity
import com.example.shine.data.local.entity.toDomain
import com.example.shine.data.local.entity.toEntity
import com.example.shine.data.remote.ChatApi
import com.example.shine.data.remote.dto.toDomain
import com.example.shine.data.remote.dto.toEntity
import com.example.shine.data.sync.ChannelSyncScheduler
import com.example.shine.domain.model.Channel
import com.example.shine.domain.model.SyncStatus
import com.example.shine.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val channelDao: ChannelDao,
    private val channelSyncScheduler: ChannelSyncScheduler,
) : ChannelRepository {

    override suspend fun getChannels(): Result<Unit> = suspendRunCatching {
        val responseList = api.getChannels()
        val serverEntities = responseList.map { it.toEntity() }
        channelDao.replaceSyncedChannels(serverEntities)
    }

    override suspend fun getRecentChannels(limit: Int): List<Channel> =
        channelDao.getChannels(limit).map { it.toDomain() }

    override suspend fun getChannel(id: String): Result<Unit> = suspendRunCatching {
        val channel = api.getConversation(id).toDomain()
        channelDao.upsertChannel(channel.toEntity())
    }

    override suspend fun createChannel(name: String): Result<Unit> = suspendRunCatching {
        channelDao.upsertChannel(
            ChannelEntity(
                name = name,
                syncStatus = SyncStatus.PENDING,
            )
        )
        channelSyncScheduler.scheduleImmediateSync()
    }

    override suspend fun retrySync(channelId: String): Result<Unit> = suspendRunCatching {
        val channel = channelDao.getChannel(channelId) ?: return@suspendRunCatching
        channelDao.upsertChannel(
            channel.copy(
                syncStatus = SyncStatus.PENDING,
                syncErrorMessage = null,
            )
        )
        channelSyncScheduler.scheduleImmediateSync()
    }

    override suspend fun deleteChannel(channelId: String): Result<Unit> = suspendRunCatching {
        channelDao.deleteChannel(channelId)
    }

    override fun getChannelsFlow(): Flow<List<Channel>> = getChannelsFlow(limit = -1)

    override fun getChannelsFlow(limit: Int): Flow<List<Channel>> =
        channelDao.observeChannels(limit).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getChannelFlow(id: String): Flow<Channel?> =
        channelDao.observeChannel(id).map { it?.toDomain() }

    override suspend fun clear() {
        channelDao.clearAll()
    }
}
