package com.example.shine.data.local

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.shine.domain.model.AppErrorEnum
import com.example.shine.domain.model.AppException
import dagger.hilt.android.qualifiers.ApplicationContext
import okio.ByteString
import okio.ByteString.Companion.toByteString
import javax.inject.Inject
import javax.inject.Singleton

data class ResolvedFile(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteString,
)

@Singleton
open class ContentUriResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    open fun resolve(uri: Uri): ResolvedFile {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

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
            val ext = try {
                MimeTypeMap.getSingleton()?.getExtensionFromMimeType(mimeType) ?: "bin"
            } catch (_: Exception) {
                "bin"
            }
            fileName = "file_${System.currentTimeMillis()}.$ext"
        }

        val bytes = try {
            contentResolver.openInputStream(uri)?.use { it.readBytes().toByteString() }
        } catch (e: Exception) {
            throw AppException(type = AppErrorEnum.UNKNOWN, cause = e)
        } ?: throw AppException(AppErrorEnum.UNKNOWN)

        return ResolvedFile(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes,
        )
    }
}
