package com.example.shine.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.shine.core.database.entity.ChannelEntity
import com.example.shine.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels ORDER BY createdAt DESC, lastActivityAt DESC LIMIT :limit")
    fun observeChannels(limit: Int = -1): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE localId = :idOrLocalId OR id = :idOrLocalId LIMIT 1")
    fun observeChannel(idOrLocalId: String): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels ORDER BY createdAt DESC, lastActivityAt DESC LIMIT :limit")
    suspend fun getChannels(limit: Int = -1): List<ChannelEntity>

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
