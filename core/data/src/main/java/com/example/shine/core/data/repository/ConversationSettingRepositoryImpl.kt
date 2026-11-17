package com.example.shine.core.data.repository

import com.example.shine.core.common.util.suspendRunCatching
import com.example.shine.core.database.dao.ConversationSettingDao
import com.example.shine.core.database.entity.ConversationSettingEntity
import com.example.shine.core.database.entity.toDomain
import com.example.shine.core.model.ConversationSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationSettingRepositoryImpl @Inject constructor(
    private val conversationSettingDao: ConversationSettingDao,
) : ConversationSettingRepository {

    override fun getConversationSettingFlow(conversationId: String): Flow<ConversationSetting?> =
        conversationSettingDao.observeConversationSetting(conversationId).map { it?.toDomain() }

    override suspend fun setNotificationEnabled(conversationId: String, enabled: Boolean): Result<Unit> =
        suspendRunCatching {
            conversationSettingDao.upsertConversationSetting(
                ConversationSettingEntity(
                    conversationId = conversationId,
                    isNotificationEnabled = enabled,
                )
            )
        }
}
