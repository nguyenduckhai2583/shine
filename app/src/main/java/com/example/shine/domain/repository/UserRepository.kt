package com.example.shine.domain.repository

import android.net.Uri
import com.example.shine.domain.model.User

interface UserRepository {
    suspend fun getProfile(): Result<Unit>
    suspend fun updateAvatar(uri: Uri): Result<User>
    suspend fun getChatUsers(workspaceId: String? = null): Result<Unit>
    suspend fun clear() {}
}
