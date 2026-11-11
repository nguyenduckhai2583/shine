package com.example.shine.core.network

import com.example.shine.core.network.dto.WorkspaceDto
import retrofit2.http.GET

interface WorkspaceApi {
    @GET("workspace-services/workspaces/me")
    suspend fun getWorkspaces(): List<WorkspaceDto>
}
