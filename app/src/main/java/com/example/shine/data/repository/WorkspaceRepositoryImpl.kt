package com.example.shine.data.repository

import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.local.dao.WorkspaceDao
import com.example.shine.data.local.entity.toDomain
import com.example.shine.data.local.entity.toEntity
import com.example.shine.data.remote.WorkspaceApi
import com.example.shine.data.remote.dto.toDomain
import com.example.shine.domain.model.Workspace
import com.example.shine.domain.repository.WorkspaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceRepositoryImpl @Inject constructor(
    private val api: WorkspaceApi,
    private val local: SessionLocalDataSource,
    private val workspaceDao: WorkspaceDao,
) : WorkspaceRepository {

    override val workspaces: Flow<List<Workspace>> = workspaceDao.observeWorkspaces()
        .map { entities -> entities.map { it.toDomain() } }

    override val selectedWorkspaceId: Flow<String?> = local.session.map { it?.workspaceId }

    override suspend fun fetchWorkspaces(): List<Workspace> {
        return try {
            val remoteWorkspaces = api.getWorkspaces().map { it.toDomain() }
            workspaceDao.replaceWorkspaces(remoteWorkspaces.map { it.toEntity() })
            remoteWorkspaces
        } catch (e: Exception) {
            val localWorkspaces = workspaceDao.getWorkspaces().map { it.toDomain() }
            localWorkspaces.ifEmpty { throw e }
        }
    }

    override suspend fun selectWorkspace(workspaceId: String) {
        local.setWorkspaceId(workspaceId)
    }

    override suspend fun clear() {
        workspaceDao.clear()
    }
}
