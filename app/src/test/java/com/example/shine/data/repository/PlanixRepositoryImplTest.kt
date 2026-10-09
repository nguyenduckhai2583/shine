package com.example.shine.data.repository

import com.example.shine.data.local.PlanixDataStore
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
    private val dataStore = PlanixDataStore()
    private val repository = PlanixRepositoryImpl(api, dataStore)

    @Test
    fun getProjectsPagingFlow_returnsFlow() = runTest {
        val pagingFlow = repository.getProjectsPagingFlow()
        assertNotNull(pagingFlow)
    }

    private class FakePlanixApi : PlanixApi {
        var projectsToReturn: List<ProjectDto> = emptyList()
        var shouldFail: Boolean = false

        override suspend fun getProjectsPagination(
            key: String?,
            page: Int?,
            limit: Int?,
        ): PaginationResponse<ProjectDto> {
            if (shouldFail) throw AppException(AppError.NO_INTERNET)
            return PaginationResponse(
                items = projectsToReturn,
                total = projectsToReturn.size,
            )
        }
    }
}
