package com.example.shine.domain.repository

import android.net.Uri
import com.example.shine.domain.model.User

interface UserRepository {
    suspend fun getProfile(): Result<User>
    suspend fun updateAvatar(uri: Uri): Result<User>
}
