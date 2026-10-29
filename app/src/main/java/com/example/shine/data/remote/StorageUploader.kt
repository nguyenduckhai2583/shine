package com.example.shine.data.remote

import com.example.shine.core.di.Unauthenticated
import com.example.shine.domain.model.AppErrorEnum
import com.example.shine.domain.model.AppException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class StorageUploader @Inject constructor(
    @Unauthenticated private val unauthenticatedClient: OkHttpClient,
) {
    open fun upload(
        url: String,
        fields: Map<String, String>,
        fileName: String,
        mimeType: String,
        bytes: ByteString,
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
            throw AppException(AppErrorEnum.NO_INTERNET, cause = e)
        }

        response.use {
            if (!it.isSuccessful) {
                throw AppException(AppErrorEnum.SERVER)
            }
        }
    }
}
