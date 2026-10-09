package com.example.shine.ui.planix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.R
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.Project
import com.example.shine.domain.repository.PlanixRepository
import com.example.shine.ui.common.UiText
import com.example.shine.ui.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class PlanixListUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val errorMessage: UiText? = null,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlanixListViewModel @Inject constructor(
    private val planixRepository: PlanixRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")

    val uiState: StateFlow<PlanixListUiState> = _query
        .debounce(300L.milliseconds)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { keyword ->
            flow {
                emit(PlanixListUiState(isLoading = true, searchQuery = _query.value))
                val result = planixRepository.getProjects(searchKey = keyword.ifEmpty { null })
                emit(
                    PlanixListUiState(
                        projects = result,
                        isLoading = false,
                        searchQuery = _query.value,
                    )
                )
            }.catch { e ->
                val error = (e as? AppException)?.toUiText()
                    ?: UiText.Resource(R.string.error_unknown)
                emit(
                    PlanixListUiState(
                        isLoading = false,
                        searchQuery = _query.value,
                        errorMessage = error,
                    )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlanixListUiState(isLoading = true),
        )

    fun onSearchQueryChanged(newQuery: String) {
        _query.value = newQuery
    }

    fun loadProjects() {
        onSearchQueryChanged(_query.value)
    }
}
