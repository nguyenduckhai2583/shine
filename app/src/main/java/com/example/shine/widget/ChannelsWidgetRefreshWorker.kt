package com.example.shine.widget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.shine.core.datastore.SessionLocalDataSource
import com.example.shine.core.database.dao.ChannelDao
import com.example.shine.core.network.ChatApi
import com.example.shine.core.network.dto.toEntity
import com.example.shine.core.model.isSignedIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class ChannelsWidgetRefreshWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val channelDao: ChannelDao,
    private val chatApi: ChatApi,
    private val sessionLocalDataSource: SessionLocalDataSource,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val session = sessionLocalDataSource.session.firstOrNull()
        if (!session.isSignedIn) {
            ChannelsWidgetUpdater.update(appContext)
            return Result.success()
        }

        return try {
            val responseList = chatApi.getChannels()
            val serverEntities = responseList.map { it.toEntity() }
            channelDao.replaceSyncedChannels(serverEntities)
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
