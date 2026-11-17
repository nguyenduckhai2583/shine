package com.example.shine.core.data.repository

import com.example.shine.core.common.error.AppErrorEnum
import com.example.shine.core.common.error.AppException
import com.example.shine.core.data.source.local.UserLocalDataSource
import com.example.shine.core.database.dao.DirectMessageDao
import com.example.shine.core.database.dao.UserDao
import com.example.shine.core.database.entity.DirectMessageEntity
import com.example.shine.core.database.entity.UserEntity
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.model.DirectMessage
import com.example.shine.core.model.Session
import com.example.shine.core.model.User
import com.example.shine.core.network.ChatApi
import com.example.shine.core.network.dto.ChannelDto
import com.example.shine.core.network.dto.DirectMessageDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class DirectMessageRepositoryImplTest {

    private val chatApi = FakeChatApi()
    private val userDao = FakeUserDao()
    private val userLocal = UserLocalDataSource(userDao)
    private val sessionLocal = mock(SessionLocalDataSource::class.java)
    private val directMessageDao = FakeDirectMessageDao()

    private val repository = DirectMessageRepositoryImpl(
        chatApi = chatApi,
        userLocalDataSource = userLocal,
        sessionLocalDataSource = sessionLocal,
        directMessageDao = directMessageDao,
    )

    @Test
    fun getDirectMessages_success_mapsAndSavesToDao() = runTest {
        `when`(sessionLocal.session).thenReturn(flowOf(Session("token", user = User("me"))))
        userDao.users.add(UserEntity(id = "user-2", firstName = "Alice", lastName = "Smith"))
        chatApi.dmsToReturn = listOf(
            DirectMessageDto(id = "dm-1", name = "Team Alpha", lastActivityAt = 100L),
            DirectMessageDto(id = "dm-2", recipientIds = listOf("user-2"), lastActivityAt = 200L),
        )

        val result = repository.getDirectMessages()
        assertTrue(result.isSuccess)

        val cached = directMessageDao.getDirectMessages()
        assertEquals(2, cached.size)
        // sorted descending by time
        assertEquals("dm-2", cached[0].id)
        assertEquals("Alice Smith", cached[0].name)
        assertEquals("dm-1", cached[1].id)
    }

    @Test
    fun getDirectMessages_apiFails_returnsFailure() = runTest {
        `when`(sessionLocal.session).thenReturn(flowOf(null))
        chatApi.shouldFail = true

        val result = repository.getDirectMessages()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
    }

    @Test
    fun getDirectMessageFlow_observesSpecificMessage() = runTest {
        directMessageDao.upsertDirectMessage(
            DirectMessageEntity(id = "dm-1", name = "Team Alpha", time = 100L)
        )

        val message = repository.getDirectMessageFlow("dm-1").first()
        assertEquals("dm-1", message?.id)
        assertEquals("Team Alpha", message?.name)

        val notFound = repository.getDirectMessageFlow("dm-unknown").first()
        assertNull(notFound)
    }

    @Test
    fun onDirectMessageCreated_upsertsToDao() = runTest {
        val newDm = DirectMessage(id = "dm-3", name = "Direct Chat", time = 300L)
        repository.onDirectMessageCreated(newDm)

        val saved = directMessageDao.getDirectMessage("dm-3")
        assertEquals("dm-3", saved?.id)
        assertEquals("Direct Chat", saved?.name)
    }

    @Test
    fun clear_clearsDao() = runTest {
        directMessageDao.upsertDirectMessage(
            DirectMessageEntity(id = "1", name = "DM", time = 10L)
        )
        repository.clear()
        assertTrue(directMessageDao.getDirectMessages().isEmpty())
    }

    private class FakeDirectMessageDao : DirectMessageDao {
        private val messagesFlow = MutableStateFlow<List<DirectMessageEntity>>(emptyList())

        override fun observeDirectMessages(): Flow<List<DirectMessageEntity>> = messagesFlow

        override fun observeDirectMessage(id: String): Flow<DirectMessageEntity?> =
            messagesFlow.map { list -> list.find { it.id == id } }

        override suspend fun getDirectMessages(): List<DirectMessageEntity> = messagesFlow.value

        override suspend fun getDirectMessage(id: String): DirectMessageEntity? =
            messagesFlow.value.find { it.id == id }

        override suspend fun upsertDirectMessage(message: DirectMessageEntity) {
            val current = messagesFlow.value.toMutableList()
            val index = current.indexOfFirst { it.id == message.id }
            if (index >= 0) {
                current[index] = message
            } else {
                current.add(0, message)
            }
            messagesFlow.value = current
        }

        override suspend fun upsertDirectMessages(messages: List<DirectMessageEntity>) {
            val current = messagesFlow.value.toMutableList()
            messages.forEach { msg ->
                val index = current.indexOfFirst { it.id == msg.id }
                if (index >= 0) {
                    current[index] = msg
                } else {
                    current.add(msg)
                }
            }
            messagesFlow.value = current
        }

        override suspend fun deleteDirectMessage(id: String) {
            messagesFlow.value = messagesFlow.value.filterNot { it.id == id }
        }

        override suspend fun clearAll() {
            messagesFlow.value = emptyList()
        }

        override suspend fun replaceDirectMessages(messages: List<DirectMessageEntity>) {
            clearAll()
            upsertDirectMessages(messages)
        }
    }

    private class FakeChatApi : ChatApi {
        var dmsToReturn: List<DirectMessageDto> = emptyList()
        var shouldFail: Boolean = false

        override suspend fun getChannels(): List<ChannelDto> = emptyList()

        override suspend fun createChannel(request: com.example.shine.core.network.dto.CreateChannelRequestDto): ChannelDto =
            ChannelDto(id = "c1", name = request.name)

        override suspend fun getConversation(id: String): ChannelDto = ChannelDto(id = id)

        override suspend fun getDirectMessages(): List<DirectMessageDto> {
            if (shouldFail) throw AppException(AppErrorEnum.NO_INTERNET)
            return dmsToReturn
        }
    }

    private class FakeUserDao : UserDao {
        val users = mutableListOf<UserEntity>()
        override fun observeUsers(): Flow<List<UserEntity>> = flowOf(users)
        override suspend fun getUsers(): List<UserEntity> = users
        override suspend fun getUser(id: String): UserEntity? = users.find { it.id == id }
        override suspend fun upsertUsers(users: List<UserEntity>) {
            this.users.removeAll { u -> users.any { it.id == u.id } }
            this.users.addAll(users)
        }
        override suspend fun clear() {
            users.clear()
        }
    }
}
