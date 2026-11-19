package com.example.shine.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.shine.core.common.di.ApplicationScope
import com.example.shine.core.database.dao.ChannelDao
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.model.isSignedIn
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ChannelsWidgetSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val sessionManager: SessionManager,
    private val channelDao: ChannelDao,
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

    fun start() {
        schedulePeriodicWidgetRefresh()

        scope.launch {
            sessionManager.session
                .map { it.isSignedIn }
                .distinctUntilChanged()
                .collect {
                    ChannelsWidgetUpdater.update(context)
                }
        }

        scope.launch {
            sessionManager.workspaceId
                .flatMapLatest { workspaceId ->
                    if (workspaceId != null) {
                        channelDao.observeChannels(workspaceId, limit = 3)
                    } else {
                        flowOf(emptyList())
                    }
                }
                .map { channels -> channels.map { "${it.id}_${it.name}_${it.lastActivityAt}" } }
                .distinctUntilChanged()
                .collect {
                    ChannelsWidgetUpdater.update(context)
                }
        }
    }

    private fun schedulePeriodicWidgetRefresh() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<ChannelsWidgetRefreshWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            ChannelsWidgetRefreshWorker.PERIODIC_15_MIN_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest,
        )
    }
}
