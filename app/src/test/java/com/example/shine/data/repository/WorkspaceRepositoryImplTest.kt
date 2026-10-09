package com.example.shine.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.local.WorkspaceDataStore
import com.example.shine.data.remote.WorkspaceApi
import com.example.shine.data.remote.dto.FileDto
import com.example.shine.data.remote.dto.WorkspaceDto
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
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
    private lateinit var workspaceDataStore: WorkspaceDataStore
    private lateinit var fakeApi: FakeWorkspaceApi
    private lateinit var repository: WorkspaceRepositoryImpl

    @Before
    fun setUp() {
        local = SessionLocalDataSource(
            PreferenceDataStoreFactory.create(scope = scope) {
                tempFolder.newFile("test_session.preferences_pb")
            },
        )
        workspaceDataStore = WorkspaceDataStore()
        fakeApi = FakeWorkspaceApi()
        repository = WorkspaceRepositoryImpl(fakeApi, local, workspaceDataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun fetchWorkspaces_success_savesToDataStoreAndReturnsDomainModels() = runTest {
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

    private class FakeWorkspaceApi : WorkspaceApi {
        var workspacesToReturn = emptyList<WorkspaceDto>()
        var shouldFail = false

        override suspend fun getWorkspaces(): List<WorkspaceDto> {
            if (shouldFail) throw AppException(AppError.NO_INTERNET)
            return workspacesToReturn
        }
    }
}
