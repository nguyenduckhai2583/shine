package com.example.shine.domain.repository

import com.example.shine.domain.model.Workspace
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    val workspaces: Flow<List<Workspace>>
    val selectedWorkspaceId: Flow<String?>
    suspend fun fetchWorkspaces(): List<Workspace>
    suspend fun selectWorkspace(workspaceId: String)
    suspend fun clear()
}
