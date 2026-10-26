package com.example.shine.data.local

import android.net.Uri

data class ImageData(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)

interface ImageReader {
    fun read(uri: Uri): ImageData
}
