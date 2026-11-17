package com.example.shine.feature.planix

import androidx.lifecycle.viewModelScope
import com.example.shine.core.common.base.BaseViewModel
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.data.repository.PlanixRepository
import com.example.shine.core.model.Project
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface PlanixDetailUiState {
    data object Loading : PlanixDetailUiState
    data class Success(val project: Project) : PlanixDetailUiState
    data class Error(val message: UiText) : PlanixDetailUiState
}

@HiltViewModel(assistedFactory = PlanixDetailViewModel.Factory::class)
class PlanixDetailViewModel @AssistedInject constructor(
    @Assisted private val projectId: String,
    planixRepository: PlanixRepository,
) : BaseViewModel() {

    val uiState: StateFlow<PlanixDetailUiState> = planixRepository
        .getProjectById(projectId)
        .map { project ->
            if (project != null) {
                PlanixDetailUiState.Success(project)
            } else {
                PlanixDetailUiState.Error(UiText.Resource(R.string.error_project_not_found))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlanixDetailUiState.Loading,
        )

    @AssistedFactory
    interface Factory {
        fun create(projectId: String): PlanixDetailViewModel
    }
}
