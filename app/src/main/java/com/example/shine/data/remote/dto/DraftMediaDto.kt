package com.example.shine.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DraftMediaRequest(
    val name: String,
    val mimeType: String,
)

@Serializable
data class DraftMediaResponse(
    val id: String,
    val name: String? = null,
    val path: String? = null,
    val presignedPost: PresignedPostDto? = null,
    val presignedUrl: String? = null,
)

@Serializable
data class PresignedPostDto(
    val url: String,
    val fields: String? = null,
)

fun PresignedPostDto.extractFields(json: Json): Map<String, String> {
    val rawFields = fields ?: return emptyMap()
    return runCatching {
        json.decodeFromString<Map<String, String>>(rawFields)
    }.getOrDefault(emptyMap())
}
