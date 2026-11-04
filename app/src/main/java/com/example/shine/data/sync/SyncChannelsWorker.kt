package com.example.shine.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.shine.R
import com.example.shine.data.local.dao.ChannelDao
import com.example.shine.data.local.entity.ChannelEntity
import com.example.shine.data.remote.ChatApi
import com.example.shine.data.remote.dto.CreateChannelRequestDto
import com.example.shine.domain.model.AppErrorEnum
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.SyncStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.IOException

@HiltWorker
class SyncChannelsWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val channelDao: ChannelDao,
    private val chatApi: ChatApi,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        val pendingChannels = channelDao.getChannelsBySyncStatus(SyncStatus.PENDING)
        if (pendingChannels.isEmpty()) {
            return@coroutineScope Result.success()
        }

        val results = pendingChannels.map { channel ->
            async { syncChannel(channel) }
        }.awaitAll()

        val hasRetry = results.any { it is SyncResult.Retry }
        if (hasRetry) Result.retry() else Result.success()
    }

    private suspend fun syncChannel(channel: ChannelEntity): SyncResult {
        return try {
            val request = CreateChannelRequestDto(
                name = channel.name,
                isPrivate = channel.isPrivate,
                isEncrypted = channel.isEncrypted,
            )
            val response = chatApi.createChannel(request)
            channelDao.markSynced(localId = channel.localId, serverId = response.id)
            SyncResult.Success
        } catch (e: Exception) {
            resolveSyncFailure(channel.localId, e)
        }
    }

    private suspend fun resolveSyncFailure(localId: String, error: Exception): SyncResult {
        return if (error.isTransientError()) {
            SyncResult.Retry
        } else {
            val message = when (error) {
                is AppException -> error.errorMessage
                else -> error.message
            } ?: appContext.getString(R.string.channel_create_failed)

            channelDao.markSyncError(localId = localId, errorMessage = message)
            SyncResult.Failure(message)
        }
    }

    private fun Exception.isTransientError(): Boolean = when (this) {
        is IOException -> true
        is AppException -> when (type) {
            AppErrorEnum.NO_INTERNET,
            AppErrorEnum.SERVER -> true
            AppErrorEnum.UNKNOWN -> false
        }
        else -> false
    }

    private sealed interface SyncResult {
        data object Success : SyncResult
        data object Retry : SyncResult
        data class Failure(val message: String) : SyncResult
    }

    companion object {
        const val UNIQUE_WORK_NAME = "SYNC_PENDING_CHANNELS"
        const val DAILY_SYNC_WORK_NAME = "DAILY_7AM_CHANNEL_SYNC"
    }
}

