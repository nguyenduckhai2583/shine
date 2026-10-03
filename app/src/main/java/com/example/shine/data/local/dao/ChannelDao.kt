package com.example.shine.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.shine.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels ORDER BY COALESCE(position, 9223372036854775807) ASC")
    fun getChannelsFlow(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels ORDER BY COALESCE(position, 9223372036854775807) ASC")
    suspend fun getChannels(): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE id = :id")
    fun getChannelFlow(id: String): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getChannel(id: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    @Transaction
    suspend fun replaceChannels(channels: List<ChannelEntity>) {
        clearChannels()
        insertChannels(channels)
    }
}
