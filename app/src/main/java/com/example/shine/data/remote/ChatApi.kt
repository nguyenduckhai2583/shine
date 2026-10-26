package com.example.shine.data.remote

import com.example.shine.data.remote.dto.ChannelDto
import com.example.shine.data.remote.dto.DirectMessageDto
import retrofit2.http.GET
import retrofit2.http.Path

interface ChatApi {
    @GET("chat-services/channels")
    suspend fun getChannels(): List<ChannelDto>

    @GET("chat-services/conversations/{id}")
    suspend fun getConversation(@Path("id") id: String): ChannelDto

    @GET("chat-services/direct-messages")
    suspend fun getDirectMessages(): List<DirectMessageDto>
}
