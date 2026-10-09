package com.example.shine.domain.repository

import androidx.paging.PagingData
import com.example.shine.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface PlanixRepository {
    fun getProjectsPagingFlow(searchKey: String? = null): Flow<PagingData<Project>>
}
