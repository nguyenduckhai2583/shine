package com.example.shine.data.repository

import android.net.Uri
import com.example.shine.core.di.Unauthenticated
import com.example.shine.data.local.ImageReader
import com.example.shine.data.local.SessionLocalDataSource
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val imageReader: ImageReader,
    private val userApi: UserApi,
    private val local: SessionLocalDataSource,
    @Unauthenticated private val unauthenticatedClient: OkHttpClient,
    private val json: Json,
) : UserRepository {

    override suspend fun updateAvatar(uri: Uri): User = withContext(Dispatchers.IO) {
        val imageData = imageReader.read(uri)
        val fileName = imageData.fileName
        val mimeType = imageData.mimeType
        val bytes = imageData.bytes

        // 1. Request draft avatar to obtain draft ID and S3 presigned POST metadata
        val draft = userApi.createAvatarDraft(
            DraftMediaRequest(
                name = fileName,
                mimeType = mimeType,
            )
        )

        // 2. Upload image binary directly to S3 storage via presigned form data
        val presignedPost = draft.presignedPost
            ?: throw AppException(AppError.SERVER, "Missing presigned post configuration")

        uploadToS3(
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

    private fun uploadToS3(
        url: String,
        fields: Map<String, String>,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ) {
        val multipartBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)

        fields.forEach { (key, value) ->
            multipartBuilder.addFormDataPart(key, value)
        }

        val fileBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        multipartBuilder.addFormDataPart("file", fileName, fileBody)

        val request = Request.Builder()
            .url(url)
            .post(multipartBuilder.build())
            .build()

        val response = try {
            unauthenticatedClient.newCall(request).execute()
        } catch (e: IOException) {
            throw AppException(AppError.NO_INTERNET, cause = e)
        }

        response.use {
            if (!it.isSuccessful) {
                throw AppException(
                    AppError.SERVER,
                    serverMessage = "Failed to upload image to storage (${it.code})",
                )
            }
        }
    }
}
