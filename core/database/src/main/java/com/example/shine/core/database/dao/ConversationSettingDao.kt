package com.example.shine.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.shine.core.database.entity.ConversationSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationSettingDao {
    @Query("SELECT * FROM conversation_settings WHERE conversationId = :conversationId")
    fun observeConversationSetting(conversationId: String): Flow<ConversationSettingEntity?>

    @Query("SELECT * FROM conversation_settings WHERE conversationId = :conversationId")
    suspend fun getConversationSetting(conversationId: String): ConversationSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversationSetting(setting: ConversationSettingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversationSettings(settings: List<ConversationSettingEntity>)

    @Query("DELETE FROM conversation_settings WHERE conversationId = :conversationId")
    suspend fun deleteConversationSetting(conversationId: String)
}
