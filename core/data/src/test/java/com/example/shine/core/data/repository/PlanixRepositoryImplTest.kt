package com.example.shine.core.data.repository

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.example.shine.core.common.error.AppErrorEnum
import com.example.shine.core.common.error.AppException
import com.example.shine.core.database.ShineDatabase
import com.example.shine.core.database.dao.ChannelDao
import com.example.shine.core.database.dao.ProjectDao
import com.example.shine.core.database.dao.ProjectRemoteKeyDao
import com.example.shine.core.database.dao.UserDao
import com.example.shine.core.database.dao.WorkspaceDao
import com.example.shine.core.network.PlanixApi
import com.example.shine.core.network.dto.PaginationResponse
import com.example.shine.core.network.dto.ProjectDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PlanixRepositoryImplTest {

    private val api = FakePlanixApi()
    private val database = FakeShineDatabase()
    private val repository = PlanixRepositoryImpl(api, database)

    @Test
    fun getProjectsPagingFlow_returnsFlow() = runTest {
        val pagingFlow = repository.getProjectsPagingFlow()
        assertNotNull(pagingFlow)
    }

    @Test
    fun getProjectById_returnsProjectFromDao() = runTest {
        val projectEntity = com.example.shine.core.database.entity.ProjectEntity(
            id = "proj-1",
            name = "Project Alpha",
            prefix = "PA",
            status = "ACTIVE",
            logoUrl = null,
            leaderName = "Alice",
        )
        fakeProjectDao.projectFlow.value = projectEntity

        val project = repository.getProjectById("proj-1").first()
        assertEquals("Project Alpha", project?.name)
        assertEquals("proj-1", project?.id)
    }

    private val fakeProjectDao = FakeProjectDao()

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    private inner class FakeShineDatabase : ShineDatabase() {
        override fun workspaceDao(): WorkspaceDao = error("Not implemented")
        override fun userDao(): UserDao = error("Not implemented")
        override fun channelDao(): ChannelDao = error("Not implemented")
        override fun directMessageDao(): com.example.shine.core.database.dao.DirectMessageDao = error("Not implemented")
        override fun conversationSettingDao(): com.example.shine.core.database.dao.ConversationSettingDao = error("Not implemented")
        override fun projectDao(): ProjectDao = fakeProjectDao
        override fun projectRemoteKeyDao(): ProjectRemoteKeyDao = error("Not implemented")
        override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper = error("Not implemented")
        override fun createInvalidationTracker(): InvalidationTracker = error("Not implemented")
        override fun clearAllTables() = Unit
    }

    private class FakeProjectDao : ProjectDao {
        val projectFlow = kotlinx.coroutines.flow.MutableStateFlow<com.example.shine.core.database.entity.ProjectEntity?>(null)

        override suspend fun insertProjects(projects: List<com.example.shine.core.database.entity.ProjectEntity>) = Unit

        override fun getProjectsPagingSource(): androidx.paging.PagingSource<Int, com.example.shine.core.database.entity.ProjectEntity> {
            error("Not implemented")
        }

        override fun getProjectById(id: String): kotlinx.coroutines.flow.Flow<com.example.shine.core.database.entity.ProjectEntity?> {
            return projectFlow
        }

        override suspend fun clearProjects() = Unit
    }

    private class FakePlanixApi : PlanixApi {
        var projectsToReturn: List<ProjectDto> = emptyList()
        var shouldFail: Boolean = false

        override suspend fun getProjectsPagination(
            key: String?,
            page: Int?,
            perPage: Int?,
        ): PaginationResponse<ProjectDto> {
            if (shouldFail) throw AppException(AppErrorEnum.NO_INTERNET)
            return PaginationResponse(
                items = projectsToReturn,
                total = projectsToReturn.size,
            )
        }
    }
}
