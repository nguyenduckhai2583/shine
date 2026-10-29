package com.example.shine.data.repository

import com.example.shine.data.local.DirectMessageDataStore
import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.remote.ChatApi
import com.example.shine.data.remote.UserApi
import com.example.shine.data.remote.dto.ChannelDto
import com.example.shine.data.remote.dto.DirectMessageDto
import com.example.shine.data.remote.dto.DraftMediaRequest
import com.example.shine.data.remote.dto.DraftMediaResponse
import com.example.shine.data.remote.dto.UpdateUserRequest
import com.example.shine.data.remote.dto.UserProfileDto
import com.example.shine.domain.model.AppErrorEnum
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.DirectMessage
import com.example.shine.domain.model.Session
import com.example.shine.domain.model.User
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class DirectMessageRepositoryImplTest {

    private val chatApi = FakeChatApi()
    private val userApi = FakeUserApi()
    private val sessionLocal = mock(SessionLocalDataSource::class.java)
    private val dataStore = DirectMessageDataStore()

    private val repository = DirectMessageRepositoryImpl(
        chatApi = chatApi,
        userApi = userApi,
        sessionLocalDataSource = sessionLocal,
        directMessageDataStore = dataStore,
    )

    @Test
    fun getDirectMessages_success_mapsAndSavesToDataStore() = runTest {
        `when`(sessionLocal.session).thenReturn(flowOf(Session("token", user = User("me"))))
        chatApi.dmsToReturn = listOf(
            DirectMessageDto(id = "dm-1", name = "Team Alpha", lastActivityAt = 100L),
            DirectMessageDto(id = "dm-2", name = "Team Beta", lastActivityAt = 200L),
        )

        val result = repository.getDirectMessages().getOrThrow()

        assertEquals(2, result.size)
        // sorted descending by time
        assertEquals("dm-2", result[0].id)
        assertEquals("dm-1", result[1].id)

        val cached = dataStore.getDirectMessages()
        assertEquals(2, cached.size)
    }

    @Test
    fun getDirectMessages_apiFails_returnsCachedData() = runTest {
        `when`(sessionLocal.session).thenReturn(flowOf(null))
        dataStore.replaceDirectMessages(listOf(DirectMessage("cached-1", "Cached DM", 50L)))
        chatApi.shouldFail = true

        val result = repository.getDirectMessages().getOrThrow()
        assertEquals(1, result.size)
        assertEquals("cached-1", result[0].id)
    }

    @Test
    fun getDirectMessages_apiFails_emptyCache_returnsFailure() = runTest {
        `when`(sessionLocal.session).thenReturn(flowOf(null))
        chatApi.shouldFail = true

        val result = repository.getDirectMessages()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
    }

    @Test
    fun clear_clearsDataStore() = runTest {
        dataStore.replaceDirectMessages(listOf(DirectMessage("1", "DM", 10L)))
        repository.clear()
        assertTrue(dataStore.getDirectMessages().isEmpty())
    }

    private class FakeChatApi : ChatApi {
        var dmsToReturn: List<DirectMessageDto> = emptyList()
        var shouldFail: Boolean = false

        override suspend fun getChannels(): List<ChannelDto> = emptyList()

        override suspend fun createChannel(request: com.example.shine.data.remote.dto.CreateChannelRequestDto): ChannelDto = ChannelDto(id = "c1", name = request.name)

        override suspend fun getConversation(id: String): ChannelDto = ChannelDto(id = id)

        override suspend fun getDirectMessages(): List<DirectMessageDto> {
            if (shouldFail) throw AppException(AppErrorEnum.NO_INTERNET)
            return dmsToReturn
        }
    }

    private class FakeUserApi : UserApi {
        override suspend fun getProfile(): UserProfileDto = UserProfileDto(id = "me")

        override suspend fun createAvatarDraft(request: DraftMediaRequest): DraftMediaResponse =
            DraftMediaResponse("1")

        override suspend fun updateProfile(request: UpdateUserRequest): UserProfileDto =
            UserProfileDto(id = "me")

        override suspend fun getChatUsers(): List<UserProfileDto> = emptyList()
    }
}
