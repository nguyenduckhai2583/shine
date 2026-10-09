package com.example.shine.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.shine.data.local.ChannelDataStore
import com.example.shine.data.local.SessionLocalDataSource
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val scope = CoroutineScope(UnconfinedTestDispatcher())
    private lateinit var local: SessionLocalDataSource
    private lateinit var fakeAuthApi: FakeAuthApi
    private lateinit var fakeWorkspaceApi: FakeWorkspaceApi
    private lateinit var fakeWorkspaceDao: FakeWorkspaceDao
    private lateinit var workspaceRepository: WorkspaceRepositoryImpl
    private lateinit var channelRepository: ChannelRepositoryImpl
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        local = SessionLocalDataSource(
            PreferenceDataStoreFactory.create(scope = scope) {
                tempFolder.newFile("auth_test_session.preferences_pb")
            },
        )
        fakeAuthApi = FakeAuthApi()
        fakeWorkspaceApi = FakeWorkspaceApi()
        fakeWorkspaceDao = FakeWorkspaceDao()
        workspaceRepository = WorkspaceRepositoryImpl(fakeWorkspaceApi, local, fakeWorkspaceDao)
        channelRepository = ChannelRepositoryImpl(FakeChatApi(), ChannelDataStore())
        repository = AuthRepositoryImpl(
            api = fakeAuthApi,
            local = local,
            workspaceRepository = workspaceRepository,
            channelRepository = channelRepository,
            passwordHasher = PasswordHasher(),
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

        val session = repository.signIn("user@test.com", "password")

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

        val session = repository.signIn("user@test.com", "password")

        assertNull(session.workspaceId)
        val saved = local.session.first()
        assertNull(saved?.workspaceId)
    }

    @Test
    fun signOut_clearsLocalSession() = runTest {
        fakeWorkspaceApi.workspacesToReturn = listOf(
            WorkspaceDto(id = "ws-single", name = "Single Workspace", isActive = true),
        )
        repository.signIn("user@test.com", "password")
        assertEquals("ws-single", local.session.first()?.workspaceId)

        repository.signOut()
        assertNull(local.session.first())
    }

    private class FakeAuthApi : AuthApi {
        override suspend fun signIn(request: SignInRequest): SessionDto {
            return SessionDto(
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
        override suspend fun getConversation(id: String): ChannelDto = throw NotImplementedError()
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
}
