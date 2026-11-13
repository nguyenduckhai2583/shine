package com.example.shine.core.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

    fun scheduleImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncChannelsWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            SyncChannelsWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun scheduleDailySync(
        targetHour: Int = SyncConstants.DEFAULT_DAILY_SYNC_HOUR,
        targetMinute: Int = SyncConstants.DEFAULT_DAILY_SYNC_MINUTE,
    ) {
        val delayMillis = calculateDelayUntilNextTargetTime(targetHour, targetMinute)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<SyncChannelsWorker>(
            SyncConstants.DAILY_SYNC_REPEAT_INTERVAL_HOURS,
            TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncChannelsWorker.DAILY_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest,
        )
    }

    fun cancelSync() {
        workManager.cancelUniqueWork(SyncChannelsWorker.UNIQUE_WORK_NAME)
        workManager.cancelUniqueWork(SyncChannelsWorker.DAILY_SYNC_WORK_NAME)
    }

    internal fun calculateDelayUntilNextTargetTime(
        targetHour: Int,
        targetMinute: Int,
        now: LocalDateTime = LocalDateTime.now(),
    ): Long {
        require(targetHour in 0..23) {
            "targetHour must be between 0 and 23, but was $targetHour"
        }
        require(targetMinute in 0..59) {
            "targetMinute must be between 0 and 59, but was $targetMinute"
        }

        val targetTime = LocalTime.of(targetHour, targetMinute, 0, 0)
        var targetDateTime = now.with(targetTime)
        if (!targetDateTime.isAfter(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }
        return Duration.between(now, targetDateTime).toMillis()
    }
}
