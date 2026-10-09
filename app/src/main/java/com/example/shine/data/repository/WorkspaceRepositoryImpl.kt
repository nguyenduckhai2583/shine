package com.example.shine.data.repository

import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.local.WorkspaceDataStore
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
    private val workspaceDataStore: WorkspaceDataStore,
) : WorkspaceRepository {

    override val workspaces: Flow<List<Workspace>> = workspaceDataStore.workspacesFlow

    override val selectedWorkspaceId: Flow<String?> = local.session.map { it?.workspaceId }

    override suspend fun fetchWorkspaces(): List<Workspace> {
        return try {
            val workspaces = api.getWorkspaces().map { it.toDomain() }
            workspaceDataStore.replaceWorkspaces(workspaces)
            workspaces
        } catch (e: Exception) {
            workspaceDataStore.getWorkspaces().ifEmpty { throw e }
        }
    }

    override suspend fun selectWorkspace(workspaceId: String) {
        local.setWorkspaceId(workspaceId)
    }

    override suspend fun clear() {
        workspaceDataStore.clear()
    }
}
