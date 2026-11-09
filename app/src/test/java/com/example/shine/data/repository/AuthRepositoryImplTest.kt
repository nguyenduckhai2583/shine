package com.example.shine.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.local.dao.ChannelDao
import com.example.shine.data.local.dao.WorkspaceDao
import com.example.shine.data.local.entity.WorkspaceEntity
import com.example.shine.data.remote.AuthApi
import com.example.shine.data.remote.ChatApi
import com.example.shine.data.remote.WorkspaceApi
import com.example.shine.data.remote.dto.ChannelDto
import com.example.shine.data.remote.dto.SessionDto
import com.example.shine.data.remote.dto.SignInRequest
import com.example.shine.data.remote.dto.UserDto
import com.example.shine.data.remote.dto.WorkspaceDto
import com.example.shine.data.security.PasswordHasher
import com.example.shine.domain.model.SignInStatus
import com.example.shine.domain.model.User
import com.example.shine.domain.repository.DirectMessageRepository
import com.example.shine.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(testDispatcher)
    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private lateinit var local: SessionLocalDataSource
    private lateinit var fakeAuthApi: FakeAuthApi
    private lateinit var fakeWorkspaceApi: FakeWorkspaceApi
    private lateinit var fakeWorkspaceDao: FakeWorkspaceDao
    private lateinit var mockChannelDao: ChannelDao
    private lateinit var workspaceRepository: WorkspaceRepositoryImpl
    private lateinit var channelRepository: ChannelRepositoryImpl
    private lateinit var fakeDirectMessageRepository: FakeDirectMessageRepository
    private lateinit var fakeUserRepository: FakeUserRepository
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        local = SessionLocalDataSource(
            PreferenceDataStoreFactory.create(scope = scope) {
                tempFolder.newFile("auth_test_session.preferences_pb")
            },
            json,
        )
        fakeAuthApi = FakeAuthApi()
        fakeWorkspaceApi = FakeWorkspaceApi()
        fakeWorkspaceDao = FakeWorkspaceDao()
        workspaceRepository = WorkspaceRepositoryImpl(
            api = fakeWorkspaceApi,
            local = local,
            workspaceDao = fakeWorkspaceDao,
        )
        mockChannelDao = Mockito.mock(ChannelDao::class.java)
        val channelScheduler = Mockito.mock(com.example.shine.data.sync.ChannelSyncScheduler::class.java)
        channelRepository = ChannelRepositoryImpl(
            api = FakeChatApi(),
            channelDao = mockChannelDao,
            channelSyncScheduler = channelScheduler,
        )
        fakeDirectMessageRepository = FakeDirectMessageRepository()
        fakeUserRepository = FakeUserRepository()
        repository = AuthRepositoryImpl(
            api = fakeAuthApi,
            local = local,
            workspaceRepository = workspaceRepository,
            channelRepository = channelRepository,
            directMessageRepository = fakeDirectMessageRepository,
            userRepository = fakeUserRepository,
            passwordHasher = PasswordHasher(),
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun signIn_singleWorkspace_autoSelectsWorkspaceId() = runTest {
        fakeWorkspaceApi.workspacesToReturn = listOf(
            WorkspaceDto(id = "ws-single", name = "Single Workspace", isActive = true),
        )

        val status = repository.signIn("user@test.com", "password").getOrThrow() as SignInStatus.Authenticated
        val session = status.session

        assertEquals("ws-single", session.workspaceId)
        val saved = local.session.first()
        assertEquals("ws-single", saved?.workspaceId)
    }

    @Test
    fun signIn_multipleWorkspaces_leavesWorkspaceIdNullForSelection() = runTest {
        fakeWorkspaceApi.workspacesToReturn = listOf(
            WorkspaceDto(id = "ws-1", name = "Workspace 1", isActive = true),
            WorkspaceDto(id = "ws-2", name = "Workspace 2", isActive = true),
        )

        val status = repository.signIn("user@test.com", "password").getOrThrow() as SignInStatus.Authenticated
        val session = status.session

        assertNull(session.workspaceId)
        val saved = local.session.first()
        assertNull(saved?.workspaceId)
    }

    @Test
    fun signIn_clearsPreviousLocalDataBeforeSavingSession() = runTest {
        repository.signIn("user@test.com", "password").getOrThrow()

        Mockito.verify(mockChannelDao).clearAll()
        assertTrue(fakeDirectMessageRepository.clearCalled)
        assertTrue(fakeUserRepository.clearCalled)
    }

    @Test
    fun signIn_tmpToken_returnsTwoFactorRequired() = runTest {
        fakeAuthApi.sessionToReturn = SessionDto(
            token = "temp-token",
            isTmpToken = true,
        )

        val status = repository.signIn("user@test.com", "password").getOrThrow()
        assertTrue(status is SignInStatus.TwoFactorRequired)
        assertEquals("temp-token", (status as SignInStatus.TwoFactorRequired).tmpToken)
    }

    @Test
    fun signOut_clearsAllLocalDataAndSession() = runTest {
        fakeWorkspaceApi.workspacesToReturn = listOf(
            WorkspaceDto(id = "ws-single", name = "Single Workspace", isActive = true),
        )
        repository.signIn("user@test.com", "password").getOrThrow()
        assertEquals("ws-single", local.session.first()?.workspaceId)

        fakeDirectMessageRepository.clearCalled = false
        fakeUserRepository.clearCalled = false

        repository.signOut()

        Mockito.verify(mockChannelDao, Mockito.atLeastOnce()).clearAll()
        assertTrue(fakeWorkspaceDao.getWorkspaces().isEmpty())
        assertTrue(fakeDirectMessageRepository.clearCalled)
        assertTrue(fakeUserRepository.clearCalled)
        assertNull(local.session.first())
    }

    @Test
    fun signOut_completesEvenIfCallingJobIsCancelled() = runTest(testDispatcher) {
        fakeWorkspaceApi.workspacesToReturn = listOf(
            WorkspaceDto(id = "ws-single", name = "Single Workspace", isActive = true),
        )
        repository.signIn("user@test.com", "password").getOrThrow()

        fakeDirectMessageRepository.clearCalled = false
        fakeUserRepository.clearCalled = false

        // Launch with UNDISPATCHED so it enters signOut immediately, then cancel the job
        val job = launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            repository.signOut()
        }
        job.cancel()
        job.join()

        Mockito.verify(mockChannelDao, Mockito.atLeastOnce()).clearAll()
        assertTrue(fakeWorkspaceDao.getWorkspaces().isEmpty())
        assertTrue(fakeDirectMessageRepository.clearCalled)
        assertTrue(fakeUserRepository.clearCalled)
        assertNull(local.session.first())
    }

    private class FakeAuthApi : AuthApi {
        var sessionToReturn: SessionDto? = null

        override suspend fun signIn(request: SignInRequest): SessionDto {
            return sessionToReturn ?: SessionDto(
                token = "mock-token",
                refreshToken = "mock-refresh",
                expireAt = 123456789L,
                user = UserDto(id = "u-1", email = request.email, firstName = "Test", lastName = "User"),
            )
        }
    }

    private class FakeWorkspaceApi : WorkspaceApi {
        var workspacesToReturn = emptyList<WorkspaceDto>()
        override suspend fun getWorkspaces(): List<WorkspaceDto> = workspacesToReturn
    }

    private class FakeChatApi : ChatApi {
        override suspend fun getChannels(): List<ChannelDto> = emptyList()
        override suspend fun createChannel(request: com.example.shine.data.remote.dto.CreateChannelRequestDto): ChannelDto = throw NotImplementedError()
        override suspend fun getConversation(id: String): ChannelDto = throw NotImplementedError()
        override suspend fun getDirectMessages(): List<com.example.shine.data.remote.dto.DirectMessageDto> = emptyList()
    }

    private class FakeWorkspaceDao : WorkspaceDao {
        private val workspacesFlow = MutableStateFlow<List<WorkspaceEntity>>(emptyList())

        override fun observeWorkspaces(): Flow<List<WorkspaceEntity>> = workspacesFlow
        override suspend fun getWorkspaces(): List<WorkspaceEntity> = workspacesFlow.value
        override suspend fun getWorkspace(id: String): WorkspaceEntity? =
            workspacesFlow.value.firstOrNull { it.id == id }

        override suspend fun upsertWorkspaces(workspaces: List<WorkspaceEntity>) {
            val current = workspacesFlow.value.toMutableList()
            workspaces.forEach { entity ->
                val idx = current.indexOfFirst { it.id == entity.id }
                if (idx >= 0) current[idx] = entity else current.add(entity)
            }
            workspacesFlow.value = current
        }

        override suspend fun clear() {
            workspacesFlow.value = emptyList()
        }
    }

    private class FakeDirectMessageRepository : DirectMessageRepository {
        var clearCalled = false

        override suspend fun getDirectMessages(): Result<Unit> = Result.success(Unit)

        override fun clear() {
            clearCalled = true
        }
    }

    private class FakeUserRepository : UserRepository {
        var clearCalled = false

        override suspend fun getProfile(): Result<Unit> = Result.success(Unit)

        override suspend fun updateAvatar(uri: android.net.Uri): Result<User> = throw NotImplementedError()

        override suspend fun getChatUsers(workspaceId: String?): Result<Unit> = Result.success(Unit)

        override suspend fun clear() {
            clearCalled = true
        }
    }
}
