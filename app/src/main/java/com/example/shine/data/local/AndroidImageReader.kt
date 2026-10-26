package com.example.shine.data.local

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidImageReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImageReader {

    override fun read(uri: Uri): ImageData {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

        var fileName: String? = null
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        if (fileName.isNullOrBlank()) {
            fileName = uri.lastPathSegment?.substringAfterLast('/')
        }
        if (fileName.isNullOrBlank()) {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            fileName = "avatar_${System.currentTimeMillis()}.$ext"
        }

        val bytes = try {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            throw AppException(AppError.UNKNOWN, "Failed to read image", e)
        } ?: throw AppException(AppError.UNKNOWN, "Failed to open image stream")

        return ImageData(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes,
        )
    }
}
