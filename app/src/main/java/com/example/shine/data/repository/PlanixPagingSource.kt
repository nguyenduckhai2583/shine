package com.example.shine.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.shine.data.local.PlanixDataStore
import com.example.shine.data.remote.PlanixApi
import com.example.shine.data.remote.dto.toDomain
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Project

class PlanixPagingSource(
    private val planixApi: PlanixApi,
    private val planixDataStore: PlanixDataStore? = null,
    private val searchKey: String? = null,
) : PagingSource<Int, Project>() {

    override fun getRefreshKey(state: PagingState<Int, Project>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Project> {
        val page = params.key ?: 1
        return try {
            val response = planixApi.getProjectsPagination(
                key = searchKey?.takeIf { it.isNotBlank() },
                page = page,
                limit = params.loadSize,
            )
            val projects = response.items.orEmpty().map { it.toDomain() }

            if (page == 1 && searchKey.isNullOrBlank()) {
                planixDataStore?.replaceProjects(projects)
            }

            val nextKey = if (response.hasNextPage) page + 1 else null

            val prevKey = if (page == 1) null else page - 1

            LoadResult.Page(
                data = projects,
                prevKey = prevKey,
                nextKey = nextKey,
            )
        } catch (e: Exception) {
            val cached = if (page == 1 && searchKey.isNullOrBlank()) {
                planixDataStore?.getProjects().orEmpty()
            } else {
                emptyList()
            }

            if (cached.isNotEmpty()) {
                LoadResult.Page(
                    data = cached,
                    prevKey = null,
                    nextKey = null,
                )
            } else {
                val appException = (e as? AppException) ?: AppException(AppError.UNKNOWN, cause = e)
                LoadResult.Error(appException)
            }
        }
    }
}
