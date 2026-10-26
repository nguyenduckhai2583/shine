package com.example.shine.data.repository

import android.net.Uri
import com.example.shine.data.local.ContentUriResolver
import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.data.remote.StorageUploader
import com.example.shine.data.remote.UserApi
import com.example.shine.data.remote.dto.DraftMediaRequest
import com.example.shine.data.remote.dto.extractFields
import com.example.shine.data.remote.dto.toUpdateRequest
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import com.example.shine.domain.model.User
import com.example.shine.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val contentUriResolver: ContentUriResolver,
    private val storageUploader: StorageUploader,
    private val userApi: UserApi,
    private val local: SessionLocalDataSource,
    private val json: Json,
) : UserRepository {

    override suspend fun updateAvatar(uri: Uri): User = withContext(Dispatchers.IO) {
        val file = contentUriResolver.resolve(uri)
        val fileName = file.fileName
        val mimeType = file.mimeType
        val bytes = file.bytes

        // 1. Request draft avatar to obtain draft ID and storage presigned POST metadata
        val draft = userApi.createAvatarDraft(
            DraftMediaRequest(
                name = fileName,
                mimeType = mimeType,
            )
        )

        // 2. Upload image binary directly to storage via presigned form data
        val presignedPost = draft.presignedPost
            ?: throw AppException(AppError.SERVER, "Missing presigned post configuration")

        storageUploader.upload(
            url = presignedPost.url,
            fields = presignedPost.extractFields(json),
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes,
        )

        // 3. Save updated avatar ID to user profile
        val currentProfile = userApi.getProfile()
        val updatedProfile = userApi.updateProfile(currentProfile.toUpdateRequest(newAvatarId = draft.id))

        // 4. Resolve new avatar URL and persist locally
        val newAvatarUrl = updatedProfile.avatar?.signedUrl
            ?: draft.presignedUrl
            ?: ""

        if (newAvatarUrl.isNotBlank()) {
            local.updateAvatar(newAvatarUrl)
        }

        User(
            id = updatedProfile.id,
            email = updatedProfile.email,
            firstName = updatedProfile.firstName,
            lastName = updatedProfile.lastName,
            avatarUrl = newAvatarUrl.ifBlank { null },
        )
    }
}
