package com.example.shine.core.testing.repository

import androidx.paging.PagingData
import com.example.shine.core.data.repository.PlanixRepository
import com.example.shine.core.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakePlanixRepository : PlanixRepository {
    val projectFlow = MutableStateFlow<Project?>(null)
    var lastSearchKey: String? = "UNSET"
    var pagingDataFlow: Flow<PagingData<Project>> = flowOf(PagingData.empty())

    override fun getProjectsPagingFlow(searchKey: String?): Flow<PagingData<Project>> {
        lastSearchKey = searchKey
        return pagingDataFlow
    }

    override fun getProjectById(id: String): Flow<Project?> {
        return projectFlow
    }
}
