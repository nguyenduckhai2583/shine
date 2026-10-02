package com.example.shine.data.remote

import com.example.shine.data.remote.dto.RefreshTokenRequest
import kotlinx.serialization.json.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/** Blocking on purpose: it is executed from inside an OkHttp interceptor thread. */
interface TokenRefreshApi {
    @POST("user-services/auth/refresh-token")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<JsonObject>
}
