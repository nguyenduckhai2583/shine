package com.example.shine.core.data.repository

import androidx.paging.PagingData
import com.example.shine.core.model.Project
import kotlinx.coroutines.flow.Flow

interface PlanixRepository {
    fun getProjectsPagingFlow(searchKey: String? = null): Flow<PagingData<Project>>
}
