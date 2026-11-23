package com.example.shine.core.testing.repository

import com.example.shine.core.data.repository.WorkspaceRepository
import com.example.shine.core.model.Workspace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeWorkspaceRepository(
    initialWorkspaces: List<Workspace> = listOf(
        Workspace("ws-1", "Workspace 1", isActive = true),
        Workspace("ws-2", "Workspace 2", isActive = true),
    ),
    initialSelectedId: String? = null,
) : WorkspaceRepository {

    val workspacesFlow = MutableStateFlow<List<Workspace>>(initialWorkspaces)
    val selectedIdFlow = MutableStateFlow<String?>(initialSelectedId)

    var lastSelectedId: String? = initialSelectedId
    var shouldFail: Boolean = false
    var failureException: Throwable = RuntimeException("Failed to fetch workspaces")
    var clearCalled: Boolean = false
    var onSelectWorkspace: (() -> Unit)? = null

    override val workspaces: Flow<List<Workspace>> = workspacesFlow

    override suspend fun fetchWorkspaces(): Result<List<Workspace>> {
        if (shouldFail) return Result.failure(failureException)
        return Result.success(workspacesFlow.value)
    }

    override suspend fun selectWorkspace(workspaceId: String) {
        lastSelectedId = workspaceId
        selectedIdFlow.value = workspaceId
        onSelectWorkspace?.invoke()
    }

    override suspend fun clear() {
        clearCalled = true
        workspacesFlow.value = emptyList()
    }
}
