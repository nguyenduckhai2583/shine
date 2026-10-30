package com.example.shine.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.example.shine.data.local.ShineDatabase
import com.example.shine.data.local.dao.ProjectDao
import com.example.shine.data.local.dao.ProjectRemoteKeyDao
import com.example.shine.data.local.dao.UserDao
import com.example.shine.data.local.dao.WorkspaceDao
import com.example.shine.data.local.entity.ProjectEntity
import com.example.shine.data.local.entity.ProjectRemoteKeyEntity
import com.example.shine.data.remote.PlanixApi
import com.example.shine.data.remote.dto.PaginationResponse
import com.example.shine.data.remote.dto.ProjectDto
import com.example.shine.domain.model.AppErrorEnum
import com.example.shine.domain.model.AppException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalPagingApi::class)
class PlanixRemoteMediatorTest {

    private val api = FakePlanixApi()
    private val fakeProjectDao = FakeProjectDao()
    private val fakeRemoteKeyDao = FakeProjectRemoteKeyDao()
    private val database = FakeShineDatabase(fakeProjectDao, fakeRemoteKeyDao)

    private val config = PagingConfig(pageSize = 5, initialLoadSize = 5, enablePlaceholders = false)

    @Test
    fun initialize_returnsLaunchInitialRefresh() = runTest {
        val mediator = PlanixRemoteMediator(api, database)
        val action = mediator.initialize()
        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, action)
    }

    @Test
    fun load_refresh_success_clearsOldDataAndInsertsNewData() = runTest {
        fakeProjectDao.projects.add(ProjectEntity(id = "old-1", name = "Old Project"))
        fakeRemoteKeyDao.remoteKeys["old-1"] = ProjectRemoteKeyEntity("old-1", null, null)

        api.responseToReturn = PaginationResponse(
            items = listOf(
                ProjectDto(id = "p1", name = "Project 1"),
                ProjectDto(id = "p2", name = "Project 2"),
            ),
            total = 10,
            lastPage = 2,
            currentPage = 1,
            perPage = 5,
        )

        val mediator = PlanixRemoteMediator(api, database)
        val state = PagingState<Int, ProjectEntity>(
            pages = emptyList(),
            anchorPosition = null,
            config = config,
            leadingPlaceholderCount = 0,
        )

        val result = mediator.load(LoadType.REFRESH, state)

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        val success = result as RemoteMediator.MediatorResult.Success
        assertFalse(success.endOfPaginationReached)

        assertEquals(2, fakeProjectDao.projects.size)
        assertEquals("p1", fakeProjectDao.projects[0].id)
        assertEquals("p2", fakeProjectDao.projects[1].id)

        val key1 = fakeRemoteKeyDao.remoteKeys["p1"]
        assertNull(key1?.prevKey)
        assertEquals(2, key1?.nextKey)
    }

    @Test
    fun load_refresh_success_endOfPaginationReached() = runTest {
        api.responseToReturn = PaginationResponse(
            items = listOf(ProjectDto(id = "p1", name = "Project 1")),
            total = 1,
            lastPage = 1,
            currentPage = 1,
            perPage = 5,
        )

        val mediator = PlanixRemoteMediator(api, database)
        val state = PagingState<Int, ProjectEntity>(
            pages = emptyList(),
            anchorPosition = null,
            config = config,
            leadingPlaceholderCount = 0,
        )

        val result = mediator.load(LoadType.REFRESH, state)

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        val success = result as RemoteMediator.MediatorResult.Success
        assertTrue(success.endOfPaginationReached)

        val key = fakeRemoteKeyDao.remoteKeys["p1"]
        assertNull(key?.prevKey)
        assertNull(key?.nextKey)
    }

    @Test
    fun load_append_success_fetchesNextPage() = runTest {
        val lastItem = ProjectEntity(id = "p1", name = "Project 1")
        fakeProjectDao.projects.add(lastItem)
        fakeRemoteKeyDao.remoteKeys["p1"] = ProjectRemoteKeyEntity(
            projectId = "p1",
            prevKey = null,
            nextKey = 2,
        )

        api.responseToReturn = PaginationResponse(
            items = listOf(ProjectDto(id = "p2", name = "Project 2")),
            total = 2,
            lastPage = 2,
            currentPage = 2,
            perPage = 5,
        )

        val mediator = PlanixRemoteMediator(api, database)
        val state = PagingState(
            pages = listOf(
                PagingSource.LoadResult.Page(
                    data = listOf(lastItem),
                    prevKey = null,
                    nextKey = 2,
                ),
            ),
            anchorPosition = 0,
            config = config,
            leadingPlaceholderCount = 0,
        )

        val result = mediator.load(LoadType.APPEND, state)

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        val success = result as RemoteMediator.MediatorResult.Success
        assertTrue(success.endOfPaginationReached)
        assertEquals(2, api.lastRequestedPage)
    }

    @Test
    fun load_append_whenEndOfPaginationReached_doesNotCallApi() = runTest {
        val lastItem = ProjectEntity(id = "p1", name = "Project 1")
        fakeProjectDao.projects.add(lastItem)
        fakeRemoteKeyDao.remoteKeys["p1"] = ProjectRemoteKeyEntity(
            projectId = "p1",
            prevKey = null,
            nextKey = null,
        )

        val mediator = PlanixRemoteMediator(api, database)
        val state = PagingState<Int, ProjectEntity>(
            pages = listOf(
                PagingSource.LoadResult.Page(
                    data = listOf(lastItem),
                    prevKey = null,
                    nextKey = null,
                ),
            ),
            anchorPosition = 0,
            config = config,
            leadingPlaceholderCount = 0,
        )

        val result = mediator.load(LoadType.APPEND, state)

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        val success = result as RemoteMediator.MediatorResult.Success
        assertTrue(success.endOfPaginationReached)
        assertNull(api.lastRequestedPage)
    }

    @Test
    fun load_prepend_atBeginning_doesNotCallApi() = runTest {
        val firstItem = ProjectEntity(id = "p1", name = "Project 1")
        fakeProjectDao.projects.add(firstItem)
        fakeRemoteKeyDao.remoteKeys["p1"] = ProjectRemoteKeyEntity(
            projectId = "p1",
            prevKey = null,
            nextKey = 2,
        )

        val mediator = PlanixRemoteMediator(api, database)
        val state = PagingState(
            pages = listOf(
                PagingSource.LoadResult.Page(
                    data = listOf(firstItem),
                    prevKey = null,
                    nextKey = 2,
                ),
            ),
            anchorPosition = 0,
            config = config,
            leadingPlaceholderCount = 0,
        )

        val result = mediator.load(LoadType.PREPEND, state)

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        val success = result as RemoteMediator.MediatorResult.Success
        assertTrue(success.endOfPaginationReached)
        assertNull(api.lastRequestedPage)
    }

    @Test
    fun load_apiError_returnsMediatorResultError() = runTest {
        api.shouldFail = true

        val mediator = PlanixRemoteMediator(api, database)
        val state = PagingState<Int, ProjectEntity>(
            pages = emptyList(),
            anchorPosition = null,
            config = config,
            leadingPlaceholderCount = 0,
        )

        val result = mediator.load(LoadType.REFRESH, state)

        assertTrue(result is RemoteMediator.MediatorResult.Error)
        val error = result as RemoteMediator.MediatorResult.Error
        assertTrue(error.throwable is AppException)
        assertEquals(AppErrorEnum.NO_INTERNET, (error.throwable as AppException).type)
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    private class FakeShineDatabase(
        private val projectDao: ProjectDao,
        private val remoteKeyDao: ProjectRemoteKeyDao,
    ) : ShineDatabase() {
        override fun workspaceDao(): WorkspaceDao = error("Not implemented")
        override fun userDao(): UserDao = error("Not implemented")
        override fun projectDao(): ProjectDao = projectDao
        override fun projectRemoteKeyDao(): ProjectRemoteKeyDao = remoteKeyDao
        override suspend fun <R> runTransaction(block: suspend () -> R): R = block()

        override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper = error("Not implemented")
        override fun createInvalidationTracker(): InvalidationTracker = error("Not implemented")
        override fun clearAllTables() = Unit
    }

    private class FakeProjectDao : ProjectDao {
        val projects = mutableListOf<ProjectEntity>()

        override suspend fun insertProjects(projects: List<ProjectEntity>) {
            this.projects.addAll(projects)
        }

        override fun getProjectsPagingSource(): PagingSource<Int, ProjectEntity> = error("Not needed")

        override suspend fun clearProjects() {
            projects.clear()
        }
    }

    private class FakeProjectRemoteKeyDao : ProjectRemoteKeyDao {
        val remoteKeys = mutableMapOf<String, ProjectRemoteKeyEntity>()

        override suspend fun insertAll(remoteKeys: List<ProjectRemoteKeyEntity>) {
            remoteKeys.forEach { this.remoteKeys[it.projectId] = it }
        }

        override suspend fun getRemoteKeyByProjectId(projectId: String): ProjectRemoteKeyEntity? {
            return remoteKeys[projectId]
        }

        override suspend fun clearRemoteKeys() {
            remoteKeys.clear()
        }
    }

    private class FakePlanixApi : PlanixApi {
        var responseToReturn: PaginationResponse<ProjectDto> = PaginationResponse()
        var shouldFail: Boolean = false
        var lastRequestedPage: Int? = null

        override suspend fun getProjectsPagination(
            key: String?,
            page: Int?,
            perPage: Int?,
        ): PaginationResponse<ProjectDto> {
            lastRequestedPage = page
            if (shouldFail) throw AppException(AppErrorEnum.NO_INTERNET)
            return responseToReturn
        }
    }
}
