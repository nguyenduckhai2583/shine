package com.example.shine.data.repository

import com.example.shine.data.local.ChannelDataStore
import com.example.shine.data.remote.ChatApi
import com.example.shine.data.remote.dto.ChannelDto
import com.example.shine.data.remote.dto.CreateChannelRequestDto
import com.example.shine.domain.model.AppErrorEnum
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelRepositoryImplTest {

    private val api = FakeChatApi()
    private val dataStore = ChannelDataStore()
    private val repository = ChannelRepositoryImpl(api, dataStore)

    @Test
    fun getChannels_fetchesFromApi_savesToDataStore_andReturns() = runTest {
        val dto = ChannelDto(id = "c1", name = "General", position = 1L)
        api.channelsToReturn = listOf(dto)

        val result = repository.getChannels().getOrThrow()

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

        val result = repository.getChannels().getOrThrow()

        assertEquals(1, result.size)
        assertEquals("Cached Channel", result[0].name)
    }

    @Test
    fun getChannels_apiFails_dataStoreEmpty_returnsFailure() = runTest {
        api.shouldFail = true
        val result = repository.getChannels()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
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
    fun createChannel_callsApi_updatesDataStore_andReturnsSuccess() = runTest {
        api.createdChannelToReturn = ChannelDto(id = "new-c1", name = "team-android", isPrivate = false)

        val result = repository.createChannel(name = "team-android")
        assertTrue(result.isSuccess)
        assertEquals("team-android", api.lastCreatedRequest?.name)

        // Verify DataStore updated with new channel
        val stored = dataStore.getChannels()
        assertEquals(1, stored.size)
        assertEquals("new-c1", stored[0].id)
    }

    @Test
    fun createChannel_apiFails_returnsFailure() = runTest {
        api.shouldFail = true
        val result = repository.createChannel(name = "test")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
    }

    @Test
    fun getChannel_apiSuccess_updatesDataStoreAndReturns() = runTest {
        api.conversationToReturn = ChannelDto(id = "c-detail", name = "Detail Channel")
        val result = repository.getChannel("c-detail").getOrThrow()
        assertEquals("c-detail", result.id)
        assertEquals("Detail Channel", result.name)

        val cached = dataStore.getChannel("c-detail")
        assertEquals("Detail Channel", cached?.name)
    }

    @Test
    fun getChannel_apiFails_returnsCachedData() = runTest {
        val cached = Channel("cached-ch", "Cached", false, false, true, null, null, null)
        dataStore.updateChannel(cached)
        api.shouldFail = true

        val result = repository.getChannel("cached-ch").getOrThrow()
        assertEquals("Cached", result.name)
    }

    @Test
    fun getChannel_apiFails_emptyCache_returnsFailure() = runTest {
        api.shouldFail = true
        val result = repository.getChannel("missing")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
    }

    private class FakeChatApi : ChatApi {
        var channelsToReturn: List<ChannelDto> = emptyList()
        var conversationToReturn: ChannelDto? = null
        var createdChannelToReturn: ChannelDto? = null
        var lastCreatedRequest: CreateChannelRequestDto? = null
        var shouldFail: Boolean = false

        override suspend fun getChannels(): List<ChannelDto> {
            if (shouldFail) throw AppException(AppErrorEnum.NO_INTERNET)
            return channelsToReturn
        }

        override suspend fun createChannel(request: CreateChannelRequestDto): ChannelDto {
            if (shouldFail) throw AppException(AppErrorEnum.NO_INTERNET)
            lastCreatedRequest = request
            return createdChannelToReturn ?: ChannelDto(id = "created-id", name = request.name)
        }

        override suspend fun getConversation(id: String): ChannelDto {
            if (shouldFail) throw AppException(AppErrorEnum.NO_INTERNET)
            return conversationToReturn ?: ChannelDto(id = id, name = "Channel $id")
        }

        override suspend fun getDirectMessages(): List<com.example.shine.data.remote.dto.DirectMessageDto> {
            if (shouldFail) throw AppException(AppErrorEnum.NO_INTERNET)
            return emptyList()
        }
    }
}
