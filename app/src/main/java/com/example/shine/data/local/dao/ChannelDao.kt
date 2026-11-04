package com.example.shine.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.shine.data.local.entity.ChannelEntity
import com.example.shine.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels ORDER BY CASE WHEN position IS NULL THEN 1 ELSE 0 END, position ASC, createdAt ASC")
    fun observeChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE localId = :idOrLocalId OR id = :idOrLocalId LIMIT 1")
    fun observeChannel(idOrLocalId: String): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels ORDER BY CASE WHEN position IS NULL THEN 1 ELSE 0 END, position ASC, createdAt ASC")
    suspend fun getChannels(): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE localId = :idOrLocalId OR id = :idOrLocalId LIMIT 1")
    suspend fun getChannel(idOrLocalId: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE syncStatus = :status")
    suspend fun getChannelsBySyncStatus(status: SyncStatus = SyncStatus.PENDING): List<ChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannel(channel: ChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChannels(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET id = :serverId, syncStatus = :status, syncErrorMessage = NULL WHERE localId = :localId")
    suspend fun markSynced(localId: String, serverId: String, status: SyncStatus = SyncStatus.SYNCED)

    @Query("UPDATE channels SET syncStatus = :status, syncErrorMessage = :errorMessage WHERE localId = :localId")
    suspend fun markSyncError(localId: String, errorMessage: String?, status: SyncStatus = SyncStatus.ERROR)

    @Query("DELETE FROM channels WHERE localId = :idOrLocalId OR id = :idOrLocalId")
    suspend fun deleteChannel(idOrLocalId: String)

    @Query("DELETE FROM channels WHERE syncStatus = :status")
    suspend fun clearSyncedChannels(status: SyncStatus = SyncStatus.SYNCED)

    @Query("DELETE FROM channels")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceSyncedChannels(serverChannels: List<ChannelEntity>) {
        clearSyncedChannels(SyncStatus.SYNCED)
        upsertChannels(serverChannels)
    }
}

