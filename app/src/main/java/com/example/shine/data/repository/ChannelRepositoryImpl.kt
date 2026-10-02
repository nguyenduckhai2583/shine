package com.example.shine.data.repository

import com.example.shine.data.remote.ChatApi
import com.example.shine.data.remote.dto.toDomain
import com.example.shine.data.remote.safeApiCall
import com.example.shine.domain.model.AuthException
import com.example.shine.domain.model.Channel
import com.example.shine.domain.repository.ChannelRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val api: ChatApi,
) : ChannelRepository {

    override suspend fun getChannels(): Result<List<Channel>> = safeApiCall {
        api.getChannels().data.orEmpty()
            .sortedBy { it.position ?: Long.MAX_VALUE }
            .map { it.toDomain() }
    }

    override suspend fun getChannel(id: String): Result<Channel> = safeApiCall {
        api.getConversation(id).data?.toDomain()
            ?: throw AuthException.Unknown("Channel not found")
    }
}
