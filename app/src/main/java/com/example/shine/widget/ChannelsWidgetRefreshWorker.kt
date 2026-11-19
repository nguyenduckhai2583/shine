package com.example.shine.widget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.shine.core.database.dao.ChannelDao
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.model.isSignedIn
import com.example.shine.core.network.ChatApi
import com.example.shine.core.network.dto.toEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ChannelsWidgetRefreshWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val channelDao: ChannelDao,
    private val chatApi: ChatApi,
    private val sessionManager: SessionManager,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val session = sessionManager.getSession()
        val workspaceId = session?.workspaceId
        if (!session.isSignedIn || workspaceId == null) {
            ChannelsWidgetUpdater.update(appContext)
            return Result.success()
        }

        return try {
            val responseList = chatApi.getChannels(workspaceId)
            val serverEntities = responseList.map { it.toEntity(workspaceId) }
            channelDao.replaceSyncedChannels(workspaceId, serverEntities)
            ChannelsWidgetUpdater.update(appContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val PERIODIC_15_MIN_WORK_NAME = "PERIODIC_15_MIN_CHANNELS_WIDGET_REFRESH"
    }
}
