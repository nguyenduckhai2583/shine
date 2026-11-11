package com.example.shine.core.network

import com.example.shine.core.network.dto.SessionDto
import com.example.shine.core.network.dto.SignInRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("user-services/auth/sign-in")
    suspend fun signIn(@Body request: SignInRequest): SessionDto
}
