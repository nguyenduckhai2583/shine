package com.example.shine.data.repository

import com.example.shine.data.local.PlanixDataStore
import com.example.shine.data.remote.PlanixApi
import com.example.shine.data.remote.dto.PaginationResponse
import com.example.shine.data.remote.dto.ProjectDto
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Project
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanixRepositoryImplTest {

    private val api = FakePlanixApi()
    private val dataStore = PlanixDataStore()
    private val repository = PlanixRepositoryImpl(api, dataStore)

    @Test
    fun getProjects_fetchesFromApi_savesToDataStore_andReturns() = runTest {
        val dto = ProjectDto(
            id = "p1",
            name = "Test Project",
            prefix = "TP",
            status = "ACTIVE",
        )
        api.projectsToReturn = listOf(dto)

        val result = repository.getProjects()

        assertEquals(1, result.size)
        assertEquals("p1", result[0].id)
        assertEquals("Test Project", result[0].name)
        assertNull(result[0].leaderName)

        val stored = dataStore.getProjects()
        assertEquals(1, stored.size)
        assertEquals("p1", stored[0].id)
    }

    @Test
    fun getProjects_apiFails_returnsCachedDataFromDataStore() = runTest {
        val project = Project("p1", "Cached Project", "CP", "ACTIVE", null, "Leader")
        dataStore.replaceProjects(listOf(project))

        api.shouldFail = true

        val result = repository.getProjects()

        assertEquals(1, result.size)
        assertEquals("Cached Project", result[0].name)
    }

    @Test(expected = AppException::class)
    fun getProjects_apiFails_dataStoreEmpty_throwsException() = runTest {
        api.shouldFail = true
        repository.getProjects()
    }

    @Test
    fun getProjectsFlow_emitsDataStoreUpdates() = runTest {
        val project = Project("p1", "Flow Project", "FP", "ACTIVE", null, "Leader")
        dataStore.replaceProjects(listOf(project))

        val flowResult = repository.getProjectsFlow().first()
        assertEquals(1, flowResult.size)
        assertEquals("Flow Project", flowResult[0].name)
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
