package com.example.shine.core.data.repository

import com.example.shine.core.model.Session
import com.example.shine.core.model.SignInStatus
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val session: Flow<Session?>

    suspend fun signIn(email: String, password: String): Result<SignInStatus>

    suspend fun signOut()
}
