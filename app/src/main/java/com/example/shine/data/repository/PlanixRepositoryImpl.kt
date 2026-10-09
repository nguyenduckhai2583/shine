package com.example.shine.data.repository

import com.example.shine.data.local.PlanixDataStore
import com.example.shine.data.remote.PlanixApi
import com.example.shine.data.remote.dto.toDomain
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Project
import com.example.shine.domain.repository.PlanixRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PlanixRepositoryImpl @Inject constructor(
    private val planixApi: PlanixApi,
    private val planixDataStore: PlanixDataStore,
) : PlanixRepository {

    override fun getProjectsFlow(): Flow<List<Project>> = planixDataStore.projectsFlow

    override suspend fun getProjects(searchKey: String?, status: String?): List<Project> {
        return try {
            val response = planixApi.getProjectsPagination(
                key = searchKey?.takeIf { it.isNotBlank() },
            )
            val projects = response.items.orEmpty().map { it.toDomain() }
            planixDataStore.replaceProjects(projects)
            projects
        } catch (e: Exception) {
            val cached = planixDataStore.getProjects()
            if (cached.isNotEmpty()) {
                cached
            } else {
                throw (e as? AppException) ?: AppException(AppError.UNKNOWN)
            }
        }
    }
}
