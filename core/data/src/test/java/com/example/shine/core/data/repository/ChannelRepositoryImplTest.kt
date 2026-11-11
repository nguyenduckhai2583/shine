package com.example.shine.core.data.repository

import com.example.shine.core.common.error.AppErrorEnum
import com.example.shine.core.common.error.AppException
import com.example.shine.core.data.sync.ChannelSyncScheduler
import com.example.shine.core.database.dao.ChannelDao
import com.example.shine.core.database.entity.ChannelEntity
import com.example.shine.core.model.Channel
import com.example.shine.core.model.SyncStatus
import com.example.shine.core.network.ChatApi
import com.example.shine.core.network.dto.ChannelDto
import com.example.shine.core.network.dto.CreateChannelRequestDto
import com.example.shine.core.network.dto.DirectMessageDto
import kotlinx.coroutines.flow.Flow
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.model.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class ChannelRepositoryImplTest {

    private lateinit var api: FakeChatApi
    private lateinit var dao: FakeChannelDao
    private lateinit var syncScheduler: ChannelSyncScheduler
    private lateinit var repository: ChannelRepositoryImpl

    @Before
    fun setUp() {
        api = FakeChatApi()
        dao = FakeChannelDao()
        syncScheduler = mock(ChannelSyncScheduler::class.java)
        repository = ChannelRepositoryImpl(
            api = api,
            channelDao = dao,
            channelSyncScheduler = syncScheduler,
        )
    }

    @Test
    fun getChannels_fetchesFromApi_savesToRoom() = runTest {
        val dto = ChannelDto(id = "c1", name = "General", position = 1L)
        api.channelsToReturn = listOf(dto)

        val result = repository.getChannels()
        assertTrue(result.isSuccess)

        val daoChannels = dao.getChannels()
        assertEquals(1, daoChannels.size)
        assertEquals("c1", daoChannels[0].id)
        assertEquals("General", daoChannels[0].name)
    }

    @Test
    fun getChannels_apiFails_returnsFailure() = runTest {
        api.shouldFail = true
        val result = repository.getChannels()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
    }

    @Test
    fun getChannel_fetchesFromApi_savesToRoom() = runTest {
        val dto = ChannelDto(id = "c1", name = "General Channel")
        api.conversationToReturn = dto

        val result = repository.getChannel("c1")
        assertTrue(result.isSuccess)

        val daoChannel = dao.getChannel("c1")
        assertEquals("c1", daoChannel?.id)
        assertEquals("General Channel", daoChannel?.name)
    }

    @Test
    fun getChannel_apiFails_returnsFailure() = runTest {
        api.shouldFail = true
        val result = repository.getChannel("c1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
    }

    @Test
    fun createChannel_savesPendingLocally_andSchedulesImmediateSync() = runTest {
        val result = repository.createChannel("New Channel")
        assertTrue(result.isSuccess)

        val daoChannels = dao.getChannels()
        assertEquals(1, daoChannels.size)
        assertEquals("New Channel", daoChannels[0].name)
        assertEquals(SyncStatus.PENDING, daoChannels[0].syncStatus)

        verify(syncScheduler).scheduleImmediateSync()
    }

    @Test
    fun getChannelsFlow_emitsFromRoom() = runTest {
        val entity = ChannelEntity(localId = "c1", id = "c1", name = "Flow Channel")
        dao.upsertChannel(entity)

        val emitted = repository.getChannelsFlow().first()
        assertEquals(1, emitted.size)
        assertEquals("Flow Channel", emitted[0].name)
    }

    @Test
    fun getRecentChannels_returnsLatestChannelsLimited() = runTest {
        dao.upsertChannel(ChannelEntity(localId = "c1", name = "Old", createdAt = 1000L))
        dao.upsertChannel(ChannelEntity(localId = "c2", name = "Medium", createdAt = 2000L))
        dao.upsertChannel(ChannelEntity(localId = "c3", name = "Newest", createdAt = 3000L))
        dao.upsertChannel(ChannelEntity(localId = "c4", name = "Older", createdAt = 500L))

        val recent = repository.getRecentChannels(limit = 3)
        assertEquals(3, recent.size)
        assertEquals("Newest", recent[0].name)
        assertEquals("Medium", recent[1].name)
        assertEquals("Old", recent[2].name)
    }

    @Test
    fun clear_clearsRoom() = runTest {
        dao.upsertChannel(ChannelEntity(localId = "c1", id = "c1", name = "Test"))
        repository.clear()
        assertTrue(dao.getChannels().isEmpty())
        verify(syncScheduler).cancelSync()
    }

    private class FakeChatApi : ChatApi {
        var channelsToReturn: List<ChannelDto> = emptyList()
        var conversationToReturn: ChannelDto? = null
        var createdChannelToReturn: ChannelDto? = null
        var shouldFail: Boolean = false

        override suspend fun getChannels(): List<ChannelDto> {
            if (shouldFail) throw AppException(AppErrorEnum.SERVER, "Server error")
            return channelsToReturn
        }

        override suspend fun createChannel(request: CreateChannelRequestDto): ChannelDto {
            if (shouldFail) throw AppException(AppErrorEnum.SERVER, "Server error")
            return createdChannelToReturn ?: ChannelDto(id = "new_id", name = request.name)
        }

        override suspend fun getConversation(id: String): ChannelDto {
            if (shouldFail) throw AppException(AppErrorEnum.SERVER, "Server error")
            return conversationToReturn ?: ChannelDto(id = id, name = "Conversation $id")
        }

        override suspend fun getDirectMessages(): List<DirectMessageDto> = emptyList()
    }

    private class FakeChannelDao : ChannelDao {
        val channelList = mutableListOf<ChannelEntity>()
        private val _flow = MutableStateFlow<List<ChannelEntity>>(emptyList())

        override fun observeChannels(limit: Int): Flow<List<ChannelEntity>> =
            _flow.map { list ->
                val sorted = list.sortedWith(
                    compareByDescending<ChannelEntity> { it.createdAt }
                        .thenByDescending { it.lastActivityAt }
                )
                if (limit > 0) sorted.take(limit) else sorted
            }

        override fun observeChannel(idOrLocalId: String): Flow<ChannelEntity?> = error("Not implemented")

        override suspend fun getChannels(limit: Int): List<ChannelEntity> {
            val sorted = channelList.sortedWith(
                compareByDescending<ChannelEntity> { it.createdAt }
                    .thenByDescending { it.lastActivityAt }
            )
            return if (limit > 0) sorted.take(limit) else sorted
        }

        override suspend fun getChannel(idOrLocalId: String): ChannelEntity? =
            channelList.firstOrNull { it.id == idOrLocalId || it.localId == idOrLocalId }

        override suspend fun getChannelsBySyncStatus(status: SyncStatus): List<ChannelEntity> =
            channelList.filter { it.syncStatus == status }

        override suspend fun upsertChannel(channel: ChannelEntity) {
            channelList.removeAll { it.localId == channel.localId }
            channelList.add(channel)
            _flow.value = channelList.toList()
        }

        override suspend fun upsertChannels(channels: List<ChannelEntity>) {
            channels.forEach { upsertChannel(it) }
        }

        override suspend fun markSynced(localId: String, serverId: String, status: SyncStatus) {
            val index = channelList.indexOfFirst { it.localId == localId }
            if (index >= 0) {
                channelList[index] = channelList[index].copy(id = serverId, syncStatus = status, syncErrorMessage = null)
                _flow.value = channelList.toList()
            }
        }

        override suspend fun markSyncError(localId: String, errorMessage: String?, status: SyncStatus) {
            val index = channelList.indexOfFirst { it.localId == localId }
            if (index >= 0) {
                channelList[index] = channelList[index].copy(syncStatus = status, syncErrorMessage = errorMessage)
                _flow.value = channelList.toList()
            }
        }

        override suspend fun deleteChannel(idOrLocalId: String) {
            channelList.removeAll { it.id == idOrLocalId || it.localId == idOrLocalId }
            _flow.value = channelList.toList()
        }

        override suspend fun clearSyncedChannels(status: SyncStatus) {
            channelList.removeAll { it.syncStatus == status }
            _flow.value = channelList.toList()
        }

        override suspend fun clearAll() {
            channelList.clear()
            _flow.value = emptyList()
        }
    }
}
