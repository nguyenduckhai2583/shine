package com.example.shine.ui.planix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Project
import com.example.shine.domain.repository.PlanixRepository
import com.example.shine.ui.common.UiText
import com.example.shine.ui.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanixListUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val errorMessage: UiText? = null,
)

@HiltViewModel
class PlanixListViewModel @Inject constructor(
    private val planixRepository: PlanixRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val isLoading = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<UiText?>(null)

    val uiState: StateFlow<PlanixListUiState> = combine(
        planixRepository.getProjectsFlow(),
        searchQuery,
        isLoading,
        errorMessage,
    ) { projects, query, loading, error ->
        val filtered = if (query.isBlank()) {
            projects
        } else {
            projects.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.prefix?.contains(query, ignoreCase = true) == true
            }
        }
        PlanixListUiState(
            projects = filtered,
            isLoading = loading && projects.isEmpty(),
            searchQuery = query,
            errorMessage = error,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PlanixListUiState(isLoading = true),
    )

    init {
        loadProjects()
    }

    fun loadProjects() {
        isLoading.value = true
        errorMessage.value = null
        viewModelScope.launch {
            try {
                planixRepository.getProjects(searchKey = searchQuery.value)
            } catch (e: AppException) {
                errorMessage.value = e.toUiText()
            } finally {
                isLoading.value = false
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
        loadProjects()
    }
}
