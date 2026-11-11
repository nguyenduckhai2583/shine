package com.example.shine.core.network.dto

import com.example.shine.core.model.Project
import kotlinx.serialization.Serializable


@Serializable
data class ProjectDto(
    val id: String,
    val name: String? = null,
    val prefix: String? = null,
    val status: String? = null,
    val logoUrl: String? = null,
    val defaultLogoPath: String? = null,
    val logo: FileDto? = null,
)

@Serializable
data class FileDto(
    val signedUrl: String? = null,
    val path: String? = null,
)

fun ProjectDto.toDomain(): Project {
    val resolvedAvatarUrl = logo?.signedUrl?.takeIf { it.isNotEmpty() }
        ?: logoUrl?.takeIf { it.isNotEmpty() }
        ?: defaultLogoPath?.takeIf { it.isNotEmpty() }

    return Project(
        id = id,
        name = name.orEmpty(),
        prefix = prefix,
        status = status,
        logoUrl = resolvedAvatarUrl,
        leaderName = null,
    )
}
