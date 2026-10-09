package com.example.shine.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.shine.data.local.PlanixDataStore
import com.example.shine.data.remote.PlanixApi
import com.example.shine.domain.model.Project
import com.example.shine.domain.repository.PlanixRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PlanixRepositoryImpl @Inject constructor(
    private val planixApi: PlanixApi,
    private val planixDataStore: PlanixDataStore,
) : PlanixRepository {

    override fun getProjectsPagingFlow(searchKey: String?): Flow<PagingData<Project>> {
        return Pager(
            config = PagingConfig(
                pageSize = 5,
                initialLoadSize = 5,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                PlanixPagingSource(
                    planixApi = planixApi,
                    planixDataStore = planixDataStore,
                    searchKey = searchKey,
                )
            },
        ).flow
    }
}
