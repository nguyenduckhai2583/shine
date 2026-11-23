package com.example.shine.core.testing.repository

import com.example.shine.core.data.repository.ChannelRepository
import com.example.shine.core.model.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class FakeChannelRepository(
    initialChannels: List<Channel> = emptyList(),
) : ChannelRepository {

    val channelsFlow = MutableStateFlow<List<Channel>>(initialChannels)
    val channelFlow = MutableStateFlow<Channel?>(null)

    var shouldFail: Boolean = false
    var failureException: Throwable = Exception("Failed to connect")
    var clearCalled: Boolean = false
    var getChannelsCallCount: Int = 0
    var deletedChannelId: String? = null
    var lastId: String? = null
    var autoUpdateFlowOnGetChannel: Boolean = true
    val createdChannels = mutableListOf<Channel>()

    override fun getChannelsFlow(workspaceId: String, limit: Int): Flow<List<Channel>> = channelsFlow

    override fun getChannelFlow(id: String): Flow<Channel?> {
        return combine(channelsFlow, channelFlow) { channels, single ->
            channels.find { it.id == id } ?: single
        }
    }

    override suspend fun getRecentChannels(workspaceId: String, limit: Int): List<Channel> {
        return channelsFlow.value.take(limit)
    }

    override suspend fun getChannels(workspaceId: String): Result<Unit> {
        getChannelsCallCount++
        if (shouldFail) return Result.failure(failureException)
        return Result.success(Unit)
    }

    override suspend fun getChannel(id: String, workspaceId: String): Result<Unit> {
        lastId = id
        if (shouldFail) return Result.failure(failureException)
        if (autoUpdateFlowOnGetChannel) {
            val existing = channelsFlow.value.find { it.id == id }
            if (existing != null) {
                channelFlow.value = existing
            } else if (channelFlow.value == null) {
                channelFlow.value = Channel(
                    id = id,
                    name = "general",
                    isPrivate = false,
                    isEncrypted = false,
                    isDefault = true,
                    categoryName = null,
                    createdAt = null,
                    lastActivityAt = null,
                )
            }
        }
        return Result.success(Unit)
    }

    override suspend fun createChannel(name: String, workspaceId: String): Result<Unit> {
        if (shouldFail) return Result.failure(failureException)
        val newChannel = Channel(
            id = "c-${System.currentTimeMillis()}",
            name = name,
            isPrivate = false,
            isEncrypted = false,
            isDefault = false,
            categoryName = null,
            createdAt = System.currentTimeMillis(),
            lastActivityAt = null,
        )
        createdChannels.add(newChannel)
        channelsFlow.value += newChannel
        return Result.success(Unit)
    }

    override suspend fun deleteChannel(channelId: String): Result<Unit> {
        if (shouldFail) return Result.failure(failureException)
        deletedChannelId = channelId
        channelsFlow.value = channelsFlow.value.filterNot { it.id == channelId }
        return Result.success(Unit)
    }

    override suspend fun onChannelCreated(channel: Channel, workspaceId: String) {
        createdChannels.add(channel)
        channelsFlow.value = channelsFlow.value + channel
    }

    override suspend fun clear() {
        clearCalled = true
        channelsFlow.value = emptyList()
        channelFlow.value = null
    }
}
