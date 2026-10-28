package com.example.shine.data.repository

import com.example.shine.data.local.ChannelDataStore
import com.example.shine.data.remote.ChatApi
import com.example.shine.data.remote.dto.ChannelDto
import com.example.shine.data.remote.dto.CreateChannelRequestDto
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelRepositoryImplTest {

    private val api = FakeChatApi()
    private val dataStore = ChannelDataStore()
    private val repository = ChannelRepositoryImpl(api, dataStore)

    @Test
    fun getChannels_fetchesFromApi_savesToDataStore_andReturns() = runTest {
        val dto = ChannelDto(id = "c1", name = "General", position = 1L)
        api.channelsToReturn = listOf(dto)

        val result = repository.getChannels()

        assertEquals(1, result.size)
        assertEquals("c1", result[0].id)
        assertEquals("General", result[0].name)

        // Verify DataStore updated
        val stored = dataStore.getChannels()
        assertEquals(1, stored.size)
        assertEquals("c1", stored[0].id)
    }

    @Test
    fun getChannels_apiFails_returnsCachedDataFromDataStore() = runTest {
        val channel = Channel("c1", "Cached Channel", false, false, true, null, null, null)
        dataStore.replaceChannels(listOf(channel))

        api.shouldFail = true

        val result = repository.getChannels()

        assertEquals(1, result.size)
        assertEquals("Cached Channel", result[0].name)
    }

    @Test(expected = AppException::class)
    fun getChannels_apiFails_dataStoreEmpty_throwsException() = runTest {
        api.shouldFail = true
        repository.getChannels()
    }

    @Test
    fun getChannelsFlow_emitsDataStoreUpdates() = runTest {
        val channel = Channel("c1", "Flow Channel", false, false, true, null, null, null)
        dataStore.replaceChannels(listOf(channel))

        val flowResult = repository.getChannelsFlow().first()
        assertEquals(1, flowResult.size)
        assertEquals("Flow Channel", flowResult[0].name)
    }

    @Test
    fun createChannel_callsApi_updatesDataStore_andReturnsChannel() = runTest {
        api.createdChannelToReturn = ChannelDto(id = "new-c1", name = "team-android", isPrivate = false)

        val result = repository.createChannel(name = "team-android")

        assertEquals("new-c1", result.id)
        assertEquals("team-android", result.name)
        assertEquals("team-android", api.lastCreatedRequest?.name)

        // Verify DataStore updated with new channel
        val stored = dataStore.getChannels()
        assertEquals(1, stored.size)
        assertEquals("new-c1", stored[0].id)
    }

    @Test(expected = AppException::class)
    fun createChannel_apiFails_throwsException() = runTest {
        api.shouldFail = true
        repository.createChannel(name = "test")
    }

    private class FakeChatApi : ChatApi {
        var channelsToReturn: List<ChannelDto> = emptyList()
        var conversationToReturn: ChannelDto? = null
        var createdChannelToReturn: ChannelDto? = null
        var lastCreatedRequest: CreateChannelRequestDto? = null
        var shouldFail: Boolean = false

        override suspend fun getChannels(): List<ChannelDto> {
            if (shouldFail) throw AppException(AppError.NO_INTERNET)
            return channelsToReturn
        }

        override suspend fun createChannel(request: CreateChannelRequestDto): ChannelDto {
            if (shouldFail) throw AppException(AppError.NO_INTERNET)
            lastCreatedRequest = request
            return createdChannelToReturn ?: ChannelDto(id = "created-id", name = request.name)
        }

        override suspend fun getConversation(id: String): ChannelDto {
            if (shouldFail) throw AppException(AppError.NO_INTERNET)
            return conversationToReturn ?: ChannelDto(id = id, name = "Channel $id")
        }

        override suspend fun getDirectMessages(): List<com.example.shine.data.remote.dto.DirectMessageDto> {
            if (shouldFail) throw AppException(AppError.NO_INTERNET)
            return emptyList()
        }
    }
}
