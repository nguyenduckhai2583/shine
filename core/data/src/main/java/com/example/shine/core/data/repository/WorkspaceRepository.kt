package com.example.shine.core.data.repository

import com.example.shine.core.model.Workspace
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    val workspaces: Flow<List<Workspace>>
    val selectedWorkspaceId: Flow<String?>
    suspend fun fetchWorkspaces(): Result<List<Workspace>>
    suspend fun selectWorkspace(workspaceId: String)
    suspend fun clear()
}
