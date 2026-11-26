package com.example.shine.feature.planix

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.shine.core.common.base.BaseViewModel
import com.example.shine.core.data.repository.PlanixRepository
import com.example.shine.core.model.Project
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlanixListViewModel @Inject constructor(
    private val planixRepository: PlanixRepository,
) : BaseViewModel() {

    private val searchQuery = MutableStateFlow("")

    val projectsPagingFlow: Flow<PagingData<Project>> = searchQuery
        .debounce { query -> if (query.isEmpty()) 0.milliseconds else 300L.milliseconds }
        .flatMapLatest { keyword ->
            planixRepository.getProjectsPagingFlow(searchKey = keyword.trim().ifEmpty { null })
        }
        .cachedIn(viewModelScope)

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }
}
