package com.example.shine.ui.workspace

import com.example.shine.domain.model.Workspace
import com.example.shine.ui.common.UiText

data class WorkspaceListUiState(
    val workspaces: List<Workspace> = emptyList(),
    val selectedWorkspaceId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
) {
    val canSubmit: Boolean
        get() = selectedWorkspaceId != null && !isLoading
}
