package com.example.shine.domain.repository

import com.example.shine.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val session: Flow<Session?>

    suspend fun signIn(email: String, password: String): Result<Session>

    suspend fun signOut()
}
