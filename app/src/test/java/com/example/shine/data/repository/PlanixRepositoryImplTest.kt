package com.example.shine.data.repository

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.example.shine.data.local.ShineDatabase
import com.example.shine.data.local.dao.ProjectDao
import com.example.shine.data.local.dao.ProjectRemoteKeyDao
import com.example.shine.data.local.dao.WorkspaceDao
import com.example.shine.data.remote.PlanixApi
import com.example.shine.data.remote.dto.PaginationResponse
import com.example.shine.data.remote.dto.ProjectDto
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import kotlinx.coroutines.test.runTest
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

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    private class FakeShineDatabase : ShineDatabase() {
        override fun workspaceDao(): WorkspaceDao = error("Not implemented")
        override fun projectDao(): ProjectDao = error("Not implemented")
        override fun projectRemoteKeyDao(): ProjectRemoteKeyDao = error("Not implemented")
        override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper = error("Not implemented")
        override fun createInvalidationTracker(): InvalidationTracker = error("Not implemented")
        override fun clearAllTables() = Unit
    }

    private class FakePlanixApi : PlanixApi {
        var projectsToReturn: List<ProjectDto> = emptyList()
        var shouldFail: Boolean = false

        override suspend fun getProjectsPagination(
            key: String?,
            page: Int?,
            perPage: Int?,
        ): PaginationResponse<ProjectDto> {
            if (shouldFail) throw AppException(AppError.NO_INTERNET)
            return PaginationResponse(
                items = projectsToReturn,
                total = projectsToReturn.size,
            )
        }
    }
}
