package com.example.shine.data.remote.socket

import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.local.UserLocalDataSource
import com.example.shine.data.local.dao.UserDao
import com.example.shine.data.local.entity.UserEntity
import com.example.shine.data.remote.socket.event.ConversationCreatedEvent
import com.example.shine.data.remote.socket.model.ConvSocketMessage
import com.example.shine.data.remote.socket.model.ConversationType
import com.example.shine.domain.model.Channel
import com.example.shine.domain.model.DirectMessage
import com.example.shine.domain.model.Session
import com.example.shine.domain.model.User
import com.example.shine.domain.repository.ChannelRepository
import com.example.shine.domain.repository.DirectMessageRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationSocketMediatorTest {

    private class FakeChannelRepository : ChannelRepository {
        val createdChannels = mutableListOf<Channel>()

        override suspend fun getChannels(): Result<Unit> = Result.success(Unit)
        override suspend fun getChannel(id: String): Result<Channel> = Result.failure(Exception())
        override suspend fun createChannel(name: String): Result<Unit> = Result.success(Unit)

        override fun onChannelCreated(channel: Channel) {
            createdChannels.add(channel)
        }
    }

    private class FakeDirectMessageRepository : DirectMessageRepository {
        val createdDms = mutableListOf<DirectMessage>()

        override suspend fun getDirectMessages(): Result<List<DirectMessage>> = Result.success(emptyList())

        override fun onDirectMessageCreated(message: DirectMessage) {
            createdDms.add(message)
        }
    }

    private class FakeUserDao : UserDao {
        val users = mutableListOf<UserEntity>()
        override fun observeUsers(): Flow<List<UserEntity>> = emptyFlow()
        override suspend fun getUsers(): List<UserEntity> = users
        override suspend fun getUser(id: String): UserEntity? = users.find { it.id == id }
        override suspend fun upsertUsers(users: List<UserEntity>) {
            this.users.removeAll { u -> users.any { it.id == u.id } }
            this.users.addAll(users)
        }
        override suspend fun clear() { users.clear() }
    }

    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var channelRepository: FakeChannelRepository
    private lateinit var directMessageRepository: FakeDirectMessageRepository
    private lateinit var sessionLocal: SessionLocalDataSource
    private lateinit var userDao: FakeUserDao
    private lateinit var userLocal: UserLocalDataSource
    private lateinit var mediator: ConversationSocketMediator

    @Before
    fun setUp() {
        channelRepository = FakeChannelRepository()
        directMessageRepository = FakeDirectMessageRepository()
        userDao = FakeUserDao()
        userLocal = UserLocalDataSource(userDao)
        sessionLocal = mock(SessionLocalDataSource::class.java)

        mediator = ConversationSocketMediator(
            channelRepository = channelRepository,
            directMessageRepository = directMessageRepository,
            sessionLocalDataSource = sessionLocal,
            userLocalDataSource = userLocal,
            json = json,
        )
    }

    @Test
    fun handleEvent_channelCreated_updatesChannelRepository() = runTest {
        val rawJson = json.parseToJsonElement(
            """
            {
                "type": "CHANNEL",
                "workspaceId": "ws-1",
                "id": "c-99",
                "name": "announcements",
                "isPrivate": false,
                "isEncrypted": false,
                "isDefault": true,
                "category": { "id": "cat-1", "name": "General" }
            }
            """.trimIndent()
        ).jsonObject

        val event = ConversationCreatedEvent(
            ConvSocketMessage(
                type = ConversationType.CHANNEL,
                workspaceId = "ws-1",
                rawJson = rawJson,
            )
        )

        mediator.handleEvent(event)

        assertEquals(1, channelRepository.createdChannels.size)
        val channel = channelRepository.createdChannels[0]
        assertEquals("c-99", channel.id)
        assertEquals("announcements", channel.name)
        assertEquals("General", channel.categoryName)
        assertTrue(channel.isDefault)
    }

    @Test
    fun handleEvent_directMessageCreated_updatesDirectMessageRepository() = runTest {
        `when`(sessionLocal.session).thenReturn(flowOf(Session("token", user = User("my-user-id"))))
        userDao.users.add(UserEntity(id = "other-user-id", firstName = "Bob", lastName = "Builder"))

        val rawJson = json.parseToJsonElement(
            """
            {
                "type": "DIRECT",
                "workspaceId": "ws-1",
                "id": "dm-55",
                "recipientIds": ["my-user-id", "other-user-id"],
                "lastActivityAt": 1700000000000
            }
            """.trimIndent()
        ).jsonObject

        val event = ConversationCreatedEvent(
            ConvSocketMessage(
                type = ConversationType.DIRECT,
                workspaceId = "ws-1",
                rawJson = rawJson,
            )
        )

        mediator.handleEvent(event)

        assertEquals(1, directMessageRepository.createdDms.size)
        val dm = directMessageRepository.createdDms[0]
        assertEquals("dm-55", dm.id)
        assertEquals("Bob Builder", dm.name)
    }
}
