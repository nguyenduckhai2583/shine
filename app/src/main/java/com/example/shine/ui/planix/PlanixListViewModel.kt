package com.example.shine.ui.planix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.shine.domain.model.Project
import com.example.shine.domain.repository.PlanixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class PlanixListUiState(
    val searchQuery: String = "",
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlanixListViewModel @Inject constructor(
    private val planixRepository: PlanixRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanixListUiState())
    val uiState: StateFlow<PlanixListUiState> = _uiState.asStateFlow()

    val projectsPagingFlow: Flow<PagingData<Project>> = _uiState
        .map { it.searchQuery }
        .distinctUntilChanged()
        .debounce { query -> if (query.isEmpty()) 0.milliseconds else 300L.milliseconds }
        .flatMapLatest { keyword ->
            planixRepository.getProjectsPagingFlow(searchKey = keyword.trim().ifEmpty { null })
        }
        .cachedIn(viewModelScope)

    fun onSearchQueryChanged(newQuery: String) {
        _uiState.value = _uiState.value.copy(searchQuery = newQuery)
    }
}
