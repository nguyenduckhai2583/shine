package com.example.shine.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.shine.core.database.entity.DirectMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectMessageDao {

    @Query("SELECT * FROM direct_messages ORDER BY COALESCE(time, 0) DESC")
    fun observeDirectMessages(): Flow<List<DirectMessageEntity>>

    @Query("SELECT * FROM direct_messages WHERE id = :id LIMIT 1")
    fun observeDirectMessage(id: String): Flow<DirectMessageEntity?>

    @Query("SELECT * FROM direct_messages ORDER BY COALESCE(time, 0) DESC")
    suspend fun getDirectMessages(): List<DirectMessageEntity>

    @Query("SELECT * FROM direct_messages WHERE id = :id LIMIT 1")
    suspend fun getDirectMessage(id: String): DirectMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDirectMessage(message: DirectMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDirectMessages(messages: List<DirectMessageEntity>)

    @Query("DELETE FROM direct_messages WHERE id = :id")
    suspend fun deleteDirectMessage(id: String)

    @Query("DELETE FROM direct_messages")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceDirectMessages(messages: List<DirectMessageEntity>) {
        clearAll()
        upsertDirectMessages(messages)
    }
}
