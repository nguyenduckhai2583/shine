package com.example.shine.data.repository

import com.example.shine.data.local.dao.ChannelDao
import com.example.shine.data.local.entity.ChannelEntity
import com.example.shine.data.remote.ChatApi
import com.example.shine.data.remote.dto.ChannelDto
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelRepositoryImplTest {

    private val api = FakeChatApi()
    private val dao = FakeChannelDao()
    private val repository = ChannelRepositoryImpl(api, dao)

    @Test
    fun getChannels_fetchesFromApi_savesToDb_andReturns() = runTest {
        val dto = ChannelDto(id = "c1", name = "General", position = 1L)
        api.channelsToReturn = listOf(dto)

        val result = repository.getChannels()

        assertEquals(1, result.size)
        assertEquals("c1", result[0].id)
        assertEquals("General", result[0].name)

        // Verify DB updated
        val storedInDb = dao.getChannels()
        assertEquals(1, storedInDb.size)
        assertEquals("c1", storedInDb[0].id)
    }

    @Test
    fun getChannels_apiFails_returnsCachedDataFromDb() = runTest {
        // Pre-populate DB
        val entity = ChannelEntity(
            id = "c1",
            name = "Cached Channel",
            isPrivate = false,
            isEncrypted = false,
            isDefault = true,
            categoryName = null,
            createdAt = null,
            lastActivityAt = null,
            position = 1L,
        )
        dao.insertChannels(listOf(entity))

        // API fails
        api.shouldFail = true

        val result = repository.getChannels()

        assertEquals(1, result.size)
        assertEquals("Cached Channel", result[0].name)
    }

    @Test(expected = AppException::class)
    fun getChannels_apiFails_dbEmpty_throwsException() = runTest {
        api.shouldFail = true
        repository.getChannels()
    }

    @Test
    fun getChannelsFlow_emitsDbUpdates() = runTest {
        val entity = ChannelEntity(
            id = "c1",
            name = "Flow Channel",
            isPrivate = false,
            isEncrypted = false,
            isDefault = true,
            categoryName = null,
            createdAt = null,
            lastActivityAt = null,
            position = 1L,
        )
        dao.insertChannels(listOf(entity))

        val flowResult = repository.getChannelsFlow().first()
        assertEquals(1, flowResult.size)
        assertEquals("Flow Channel", flowResult[0].name)
    }

    private class FakeChatApi : ChatApi {
        var channelsToReturn: List<ChannelDto> = emptyList()
        var conversationToReturn: ChannelDto? = null
        var shouldFail: Boolean = false

        override suspend fun getChannels(): List<ChannelDto> {
            if (shouldFail) throw AppException(AppError.NO_INTERNET)
            return channelsToReturn
        }

        override suspend fun getConversation(id: String): ChannelDto {
            if (shouldFail) throw AppException(AppError.NO_INTERNET)
            return conversationToReturn ?: ChannelDto(id = id, name = "Channel $id")
        }
    }

    private class FakeChannelDao : ChannelDao {
        private val channelsMap = mutableMapOf<String, ChannelEntity>()
        private val flow = MutableStateFlow<List<ChannelEntity>>(emptyList())

        override fun getChannelsFlow(): Flow<List<ChannelEntity>> = flow

        override suspend fun getChannels(): List<ChannelEntity> = channelsMap.values.toList()

        override fun getChannelFlow(id: String): Flow<ChannelEntity?> {
            return MutableStateFlow(channelsMap[id])
        }

        override suspend fun getChannel(id: String): ChannelEntity? = channelsMap[id]

        override suspend fun insertChannels(channels: List<ChannelEntity>) {
            channels.forEach { channelsMap[it.id] = it }
            flow.value = channelsMap.values.toList()
        }

        override suspend fun insertChannel(channel: ChannelEntity) {
            channelsMap[channel.id] = channel
            flow.value = channelsMap.values.toList()
        }

        override suspend fun clearChannels() {
            channelsMap.clear()
            flow.value = emptyList()
        }
    }
}
