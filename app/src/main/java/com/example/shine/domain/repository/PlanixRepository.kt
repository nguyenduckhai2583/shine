package com.example.shine.domain.repository

import com.example.shine.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface PlanixRepository {
    fun getProjectsFlow(): Flow<List<Project>>
    suspend fun getProjects(searchKey: String? = null, status: String? = null): List<Project>
}
