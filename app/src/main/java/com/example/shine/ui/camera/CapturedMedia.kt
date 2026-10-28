package com.example.shine.ui.camera

import android.net.Uri

sealed interface CapturedMedia {
    val uri: Uri

    data class Photo(override val uri: Uri) : CapturedMedia
    data class Video(override val uri: Uri, val durationMs: Long = 0L) : CapturedMedia
}

enum class CameraCaptureMode {
    PHOTO,
    VIDEO,
}
