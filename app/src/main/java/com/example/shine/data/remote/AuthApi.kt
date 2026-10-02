package com.example.shine.data.remote

import com.example.shine.data.remote.dto.ApiResponse
import com.example.shine.data.remote.dto.SessionDto
import com.example.shine.data.remote.dto.SignInRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("user-services/auth/sign-in")
    suspend fun signIn(@Body request: SignInRequest): ApiResponse<SessionDto>
}
