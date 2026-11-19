package com.example.shine.core.network

import com.example.shine.core.network.dto.ChannelDto
import com.example.shine.core.network.dto.CreateChannelRequestDto
import com.example.shine.core.network.dto.DirectMessageDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatApi {
    @GET("chat-services/channels")
    suspend fun getChannels(
        @Header("x-workspace-id") workspaceId: String? = null,
    ): List<ChannelDto>

    @POST("chat-services/channels")
    suspend fun createChannel(
        @Body request: CreateChannelRequestDto,
        @Header("x-workspace-id") workspaceId: String? = null,
    ): ChannelDto

    @GET("chat-services/conversations/{id}")
    suspend fun getConversation(@Path("id") id: String): ChannelDto

    @GET("chat-services/direct-messages")
    suspend fun getDirectMessages(): List<DirectMessageDto>
}
