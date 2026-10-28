package com.example.shine.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

object CameraMediaUtils {

    fun createPhotoFile(context: Context): File {
        val imageDir = File(context.cacheDir, "images").apply { mkdirs() }
        return File.createTempFile("IMG_${System.currentTimeMillis()}_", ".jpg", imageDir)
    }

    fun createVideoFile(context: Context): File {
        val videoDir = File(context.cacheDir, "media").apply { mkdirs() }
        return File.createTempFile("VID_${System.currentTimeMillis()}_", ".mp4", videoDir)
    }

    fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    fun getVideoThumbnail(context: Context, uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.frameAtTime
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun getVideoDuration(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun formatDuration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
