package com.example.shine.data.remote

import com.example.shine.data.remote.dto.WorkspaceDto
import retrofit2.http.GET

interface WorkspaceApi {
    @GET("workspace-services/workspaces/me")
    suspend fun getWorkspaces(): List<WorkspaceDto>
}
