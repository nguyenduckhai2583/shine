package com.example.shine.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.local.dao.WorkspaceDao
import com.example.shine.data.local.entity.WorkspaceEntity
import com.example.shine.data.remote.WorkspaceApi
import com.example.shine.data.remote.dto.FileDto
import com.example.shine.data.remote.dto.WorkspaceDto
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Session
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val scope = CoroutineScope(UnconfinedTestDispatcher())
    private lateinit var local: SessionLocalDataSource
    private lateinit var fakeDao: FakeWorkspaceDao
    private lateinit var fakeApi: FakeWorkspaceApi
    private lateinit var repository: WorkspaceRepositoryImpl

    @Before
    fun setUp() {
        local = SessionLocalDataSource(
            PreferenceDataStoreFactory.create(scope = scope) {
                tempFolder.newFile("test_session.preferences_pb")
            },
        )
        fakeDao = FakeWorkspaceDao()
        fakeApi = FakeWorkspaceApi()
        repository = WorkspaceRepositoryImpl(fakeApi, local, fakeDao)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun fetchWorkspaces_success_savesToDaoAndReturnsDomainModels() = runTest {
        fakeApi.workspacesToReturn = listOf(
            WorkspaceDto(
                id = "ws-1",
                name = "Hodfords",
                isActive = true,
                subdomain = "hodfords",
                logo = FileDto(signedUrl = "https://example.com/logo.png"),
            ),
        )

        val result = repository.fetchWorkspaces()

        assertEquals(1, result.size)
        assertEquals("ws-1", result[0].id)
        assertEquals("Hodfords", result[0].name)
        assertEquals("https://example.com/logo.png", result[0].logoUrl)

        val cached = repository.workspaces.first()
        assertEquals(1, cached.size)
        assertEquals("ws-1", cached[0].id)
    }

    @Test
    fun selectWorkspace_updatesWorkspaceIdInLocalSession() = runTest {
        local.save(Session("token", null, null, null))

        repository.selectWorkspace("ws-99")

        val session = local.session.first()
        assertEquals("ws-99", session?.workspaceId)
    }

    @Test
    fun clear_emptiesWorkspacesFlow() = runTest {
        fakeApi.workspacesToReturn = listOf(
            WorkspaceDto(id = "ws-1", name = "Test", isActive = true),
        )
        repository.fetchWorkspaces()
        assertEquals(1, repository.workspaces.first().size)

        repository.clear()
        assertTrue(repository.workspaces.first().isEmpty())
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

    private class FakeWorkspaceApi : WorkspaceApi {
        var workspacesToReturn = emptyList<WorkspaceDto>()
        var shouldFail = false

        override suspend fun getWorkspaces(): List<WorkspaceDto> {
            if (shouldFail) throw AppException(AppError.NO_INTERNET)
            return workspacesToReturn
        }
    }
}
