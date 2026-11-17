package com.example.shine.core.data.repository

import com.example.shine.core.model.ConversationSetting
import kotlinx.coroutines.flow.Flow

interface ConversationSettingRepository {
    fun getConversationSettingFlow(conversationId: String): Flow<ConversationSetting?>
    suspend fun setNotificationEnabled(conversationId: String, enabled: Boolean): Result<Unit>
}
