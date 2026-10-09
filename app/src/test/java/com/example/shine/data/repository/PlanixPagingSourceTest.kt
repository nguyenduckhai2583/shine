package com.example.shine.data.repository

import androidx.paging.PagingSource
import com.example.shine.data.local.PlanixDataStore
import com.example.shine.data.remote.PlanixApi
import com.example.shine.data.remote.dto.PaginationResponse
import com.example.shine.data.remote.dto.ProjectDto
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Project
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanixPagingSourceTest {

    private val api = FakePlanixApi()
    private val dataStore = PlanixDataStore()

    @Test
    fun load_initialPage_returnsPageWithNextKeyAndSavesToDataStore() = runTest {
        val dto = ProjectDto(id = "p1", name = "Project 1")
        api.responseToReturn = PaginationResponse(
            items = listOf(dto),
            total = 10,
            lastPage = 2,
            currentPage = 1,
            perPage = 1,
        )

        val pagingSource = PlanixPagingSource(api, dataStore)
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 1,
                placeholdersEnabled = false,
            )
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(1, page.data.size)
        assertEquals("p1", page.data[0].id)
        assertNull(page.prevKey)
        assertEquals(2, page.nextKey)

        assertEquals(1, dataStore.getProjects().size)
    }

    @Test
    fun load_lastPage_returnsPageWithNullNextKey() = runTest {
        val dto = ProjectDto(id = "p2", name = "Project 2")
        api.responseToReturn = PaginationResponse(
            items = listOf(dto),
            total = 2,
            lastPage = 2,
            currentPage = 2,
            perPage = 1,
        )

        val pagingSource = PlanixPagingSource(api, dataStore)
        val result = pagingSource.load(
            PagingSource.LoadParams.Append(
                key = 2,
                loadSize = 1,
                placeholdersEnabled = false,
            )
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(1, page.data.size)
        assertEquals(1, page.prevKey)
        assertNull(page.nextKey)
    }

    @Test
    fun load_emptyResponse_returnsNullNextKey() = runTest {
        api.responseToReturn = PaginationResponse(
            items = emptyList(),
            total = 0,
            lastPage = 0,
            currentPage = 1,
            perPage = 20,
        )

        val pagingSource = PlanixPagingSource(api, dataStore)
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false,
            )
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertTrue(page.data.isEmpty())
        assertNull(page.nextKey)
    }

    @Test
    fun load_apiError_cachedDataAvailable_returnsCachedPage() = runTest {
        dataStore.replaceProjects(listOf(Project("c1", "Cached", "C", "ACTIVE", null, null)))
        api.shouldFail = true

        val pagingSource = PlanixPagingSource(api, dataStore)
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false,
            )
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(1, page.data.size)
        assertEquals("c1", page.data[0].id)
        assertNull(page.nextKey)
    }

    @Test
    fun load_apiError_dataStoreEmpty_returnsLoadResultError() = runTest {
        api.shouldFail = true

        val pagingSource = PlanixPagingSource(api, dataStore)
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false,
            )
        )

        assertTrue(result is PagingSource.LoadResult.Error)
        val error = result as PagingSource.LoadResult.Error
        assertTrue(error.throwable is AppException)
    }

    private class FakePlanixApi : PlanixApi {
        var responseToReturn: PaginationResponse<ProjectDto> = PaginationResponse()
        var shouldFail: Boolean = false

        override suspend fun getProjectsPagination(
            key: String?,
            page: Int?,
            limit: Int?,
        ): PaginationResponse<ProjectDto> {
            if (shouldFail) throw AppException(AppError.NO_INTERNET)
            return responseToReturn
        }
    }
}
