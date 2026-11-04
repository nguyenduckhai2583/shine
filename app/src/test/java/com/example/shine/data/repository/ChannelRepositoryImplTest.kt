package com.example.shine.data.repository

import com.example.shine.data.local.dao.ChannelDao
import com.example.shine.data.local.entity.ChannelEntity
import com.example.shine.data.remote.ChatApi
import com.example.shine.data.remote.dto.ChannelDto
import com.example.shine.data.remote.dto.CreateChannelRequestDto
import com.example.shine.data.remote.dto.DirectMessageDto
import com.example.shine.data.sync.ChannelSyncScheduler
import com.example.shine.domain.model.AppErrorEnum
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Channel
import com.example.shine.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    fun getChannels_fetchesFromApi_savesToRoom_andReturns() = runTest {
        val dto = ChannelDto(id = "c1", name = "General", position = 1L)
        api.channelsToReturn = listOf(dto)

        val result = repository.getChannels().getOrThrow()

        assertEquals(1, result.size)
        assertEquals("c1", result[0].id)
        assertEquals("General", result[0].name)

        val daoChannels = dao.getChannels()
        assertEquals(1, daoChannels.size)
        assertEquals("c1", daoChannels[0].id)
    }

    @Test
    fun getChannels_apiFails_returnsCachedDataFromRoom() = runTest {
        val entity = ChannelEntity(localId = "c1", id = "c1", name = "Cached Channel")
        dao.upsertChannel(entity)

        api.shouldFail = true

        val result = repository.getChannels().getOrThrow()

        assertEquals(1, result.size)
        assertEquals("Cached Channel", result[0].name)
    }

    @Test
    fun getChannels_apiFails_roomEmpty_returnsFailure() = runTest {
        api.shouldFail = true
        val result = repository.getChannels()
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
    fun clear_clearsRoom() = runTest {
        dao.upsertChannel(ChannelEntity(localId = "c1", id = "c1", name = "Test"))
        repository.clear()
        assertTrue(dao.getChannels().isEmpty())
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

        override fun observeChannels(): Flow<List<ChannelEntity>> = _flow.asStateFlow()

        override fun observeChannel(idOrLocalId: String): Flow<ChannelEntity?> = error("Not implemented")

        override suspend fun getChannels(): List<ChannelEntity> = channelList.toList()

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
