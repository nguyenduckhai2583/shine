package com.example.shine.core.data.repository

import com.example.shine.core.common.util.suspendRunCatching
import com.example.shine.core.database.dao.WorkspaceDao
import com.example.shine.core.database.entity.toDomain
import com.example.shine.core.database.entity.toEntity
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.model.Workspace
import com.example.shine.core.network.WorkspaceApi
import com.example.shine.core.network.dto.toDomain
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

    override suspend fun fetchWorkspaces(): Result<List<Workspace>> = suspendRunCatching {
        try {
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
