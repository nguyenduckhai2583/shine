package com.example.shine.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.shine.core.di.InMemoryDatabase
import com.example.shine.data.local.ShineDatabase
import com.example.shine.data.local.entity.toDomain
import com.example.shine.data.remote.PlanixApi
import com.example.shine.domain.model.Project
import com.example.shine.domain.repository.PlanixRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlanixRepositoryImpl @Inject constructor(
    private val planixApi: PlanixApi,
    @InMemoryDatabase private val database: ShineDatabase,
) : PlanixRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getProjectsPagingFlow(searchKey: String?): Flow<PagingData<Project>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
            ),
            remoteMediator = PlanixRemoteMediator(
                planixApi = planixApi,
                database = database,
                searchKey = searchKey,
            ),
            pagingSourceFactory = {
                database.projectDao().getProjectsPagingSource()
            },
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }
}
