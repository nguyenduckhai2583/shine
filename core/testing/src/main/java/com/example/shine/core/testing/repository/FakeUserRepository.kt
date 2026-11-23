package com.example.shine.core.testing.repository

import android.net.Uri
import com.example.shine.core.data.repository.UserRepository
import com.example.shine.core.model.User

class FakeUserRepository : UserRepository {
    var getProfileCalls: Int = 0
    var getChatUsersCalls: Int = 0
    var lastWorkspaceId: String? = null
    var onGetChatUsers: (() -> Unit)? = null
    var lastUpdatedUri: Uri? = null
    var shouldFail: Boolean = false
    var failureException: Throwable = RuntimeException("Network error")
    var clearCalled: Boolean = false
    var updatedUser: User = User(
        id = "1",
        email = "jane@example.com",
        firstName = "Jane",
        lastName = "Doe",
        avatarUrl = "https://example.com/new_avatar.jpg",
    )

    override suspend fun getProfile(): Result<Unit> {
        getProfileCalls++
        if (shouldFail) return Result.failure(failureException)
        return Result.success(Unit)
    }

    override suspend fun getChatUsers(workspaceId: String?): Result<Unit> {
        getChatUsersCalls++
        lastWorkspaceId = workspaceId
        onGetChatUsers?.invoke()
        if (shouldFail) return Result.failure(failureException)
        return Result.success(Unit)
    }

    override suspend fun updateAvatar(uri: Uri): Result<User> {
        lastUpdatedUri = uri
        if (shouldFail) return Result.failure(failureException)
        return Result.success(updatedUser)
    }

    override suspend fun clear() {
        clearCalled = true
    }
}
