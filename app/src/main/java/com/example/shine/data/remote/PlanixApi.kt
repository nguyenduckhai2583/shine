package com.example.shine.data.remote

import com.example.shine.data.remote.dto.PaginationResponse
import com.example.shine.data.remote.dto.ProjectDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PlanixApi {
    @GET("project-services/projects/pagination")
    suspend fun getProjectsPagination(
        @Query("key") key: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
    ): PaginationResponse<ProjectDto>
}
