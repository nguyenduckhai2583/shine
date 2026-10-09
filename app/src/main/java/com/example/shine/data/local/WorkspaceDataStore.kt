package com.example.shine.data.local

import com.example.shine.domain.model.Workspace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceDataStore @Inject constructor() {

    private val _workspacesState = MutableStateFlow<List<Workspace>>(emptyList())
    val workspacesFlow: Flow<List<Workspace>> = _workspacesState.asStateFlow()

    fun getWorkspaces(): List<Workspace> = _workspacesState.value

    fun replaceWorkspaces(workspaces: List<Workspace>) {
        _workspacesState.value = workspaces
    }

    fun clear() {
        _workspacesState.value = emptyList()
    }
}
