package com.example.shine.feature.camera

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
enum class CameraCaptureMode {
    PHOTO,
    VIDEO,
}

@Serializable
sealed interface CapturedMedia {
    val uri: String

    @Serializable
    data class Photo(override val uri: String) : CapturedMedia {
        constructor(uri: Uri) : this(uri.toString())
    }

    @Serializable
    data class Video(
        override val uri: String,
        val durationMs: Long = 0L,
    ) : CapturedMedia {
        constructor(uri: Uri, durationMs: Long = 0L) : this(uri.toString(), durationMs)
    }
}
