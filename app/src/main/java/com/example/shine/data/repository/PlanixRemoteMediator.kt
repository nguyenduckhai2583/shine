package com.example.shine.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.shine.data.local.ShineDatabase
import com.example.shine.data.local.entity.ProjectEntity
import com.example.shine.data.local.entity.ProjectRemoteKeyEntity
import com.example.shine.data.local.entity.toEntity
import com.example.shine.data.remote.PlanixApi
import com.example.shine.data.remote.dto.toDomain
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException

@OptIn(ExperimentalPagingApi::class)
class PlanixRemoteMediator(
    private val planixApi: PlanixApi,
    private val database: ShineDatabase,
    private val searchKey: String? = null,
) : RemoteMediator<Int, ProjectEntity>() {

    private val projectDao by lazy { database.projectDao() }
    private val projectRemoteKeyDao by lazy { database.projectRemoteKeyDao() }

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ProjectEntity>,
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val remoteKey = getRemoteKeyForLastItem(state)
                val nextKey = remoteKey?.nextKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKey != null)
                nextKey
            }
        }

        return try {
            val response = planixApi.getProjectsPagination(
                key = searchKey?.takeIf { it.isNotBlank() },
                page = page,
                limit = state.config.pageSize,
            )

            val projects = response.items.orEmpty().map { it.toDomain() }
            val endOfPaginationReached = !response.hasNextPage || projects.isEmpty()

            database.runTransaction {
                if (loadType == LoadType.REFRESH) {
                    projectRemoteKeyDao.clearRemoteKeys()
                    projectDao.clearProjects()
                }

                val prevKey = if (page == 1) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1

                val remoteKeys = projects.map { project ->
                    ProjectRemoteKeyEntity(
                        projectId = project.id,
                        prevKey = prevKey,
                        nextKey = nextKey,
                    )
                }

                val entities = projects.map { it.toEntity() }

                projectRemoteKeyDao.insertAll(remoteKeys)
                projectDao.insertProjects(entities)
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: Exception) {
            val appException = (e as? AppException) ?: AppException(AppError.UNKNOWN, cause = e)
            MediatorResult.Error(appException)
        }
    }

    private suspend fun getRemoteKeyForLastItem(
        state: PagingState<Int, ProjectEntity>,
    ): ProjectRemoteKeyEntity? {
        val lastItem = state.lastItemOrNull() ?: return null
        return projectRemoteKeyDao.getRemoteKeyByProjectId(lastItem.id)
    }
}
