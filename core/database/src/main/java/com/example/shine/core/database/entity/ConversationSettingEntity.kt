package com.example.shine.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.shine.core.model.ConversationSetting

@Entity(tableName = "conversation_settings")
data class ConversationSettingEntity(
    @PrimaryKey
    val conversationId: String,
    val isNotificationEnabled: Boolean = true,
)

fun ConversationSettingEntity.toDomain() = ConversationSetting(
    conversationId = conversationId,
    isNotificationEnabled = isNotificationEnabled,
)

fun ConversationSetting.toEntity() = ConversationSettingEntity(
    conversationId = conversationId,
    isNotificationEnabled = isNotificationEnabled,
)
