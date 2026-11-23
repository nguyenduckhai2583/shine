package com.example.shine.core.data.socket

import com.example.shine.core.data.source.local.UserLocalDataSource
import com.example.shine.core.database.dao.UserDao
import com.example.shine.core.database.entity.UserEntity
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.model.Session
import com.example.shine.core.network.socket.model.ConvSocketMessage
import com.example.shine.core.network.socket.model.ConversationType
import com.example.shine.core.testing.repository.FakeChannelRepository
import com.example.shine.core.testing.repository.FakeDirectMessageRepository
import com.example.shine.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ConversationSocketMediatorTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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
    private lateinit var sessionManager: SessionManager
    private val sessionFlow = MutableStateFlow<Session?>(Session(token = "token", workspaceId = "ws-1"))
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
        `when`(sessionLocal.session).thenReturn(sessionFlow)
        sessionManager = SessionManager(sessionLocal, CoroutineScope(mainDispatcherRule.testDispatcher))

        mediator = ConversationSocketMediator(
            channelRepository = channelRepository,
            directMessageRepository = directMessageRepository,
            sessionManager = sessionManager,
            userLocalDataSource = userLocal,
            json = json,
        )
    }

    @Test
    fun handleMessage_channelCreated_delegatesToChannelRepository() = runTest {
        val rawJson = """
            {
                "id": "ch_1",
                "name": "general",
                "is_private": false,
                "is_encrypted": false,
                "is_default": true
            }
        """.trimIndent()
        val socketMessage = ConvSocketMessage(
            type = ConversationType.CHANNEL,
            workspaceId = "ws-1",
            rawJson = json.parseToJsonElement(rawJson).jsonObject,
        )

        mediator.handleConversationCreated(socketMessage)

        assertEquals(1, channelRepository.createdChannels.size)
        assertEquals("ch_1", channelRepository.createdChannels[0].id)
    }

    @Test
    fun handleMessage_directCreated_delegatesToDirectMessageRepository() = runTest {
        val rawJson = """
            {
                "id": "dm_1",
                "members": [
                    {
                        "id": "u_other",
                        "firstName": "Alice"
                    }
                ]
            }
        """.trimIndent()
        val socketMessage = ConvSocketMessage(
            type = ConversationType.DIRECT,
            workspaceId = "ws-1",
            rawJson = json.parseToJsonElement(rawJson).jsonObject,
        )

        mediator.handleConversationCreated(socketMessage)

        assertEquals(1, directMessageRepository.createdDms.size)
        assertEquals("dm_1", directMessageRepository.createdDms[0].id)
    }
}
