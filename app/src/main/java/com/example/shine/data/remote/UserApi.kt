package com.example.shine.data.remote

import com.example.shine.data.remote.dto.DraftMediaRequest
import com.example.shine.data.remote.dto.DraftMediaResponse
import com.example.shine.data.remote.dto.UpdateUserRequest
import com.example.shine.data.remote.dto.UserProfileDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface UserApi {
    @GET("user-services/users/me")
    suspend fun getProfile(): UserProfileDto

    @POST("user-services/users/me/avatar/draft")
    suspend fun createAvatarDraft(@Body request: DraftMediaRequest): DraftMediaResponse

    @PUT("user-services/users/me")
    suspend fun updateProfile(@Body request: UpdateUserRequest): UserProfileDto
}
