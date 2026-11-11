package com.example.shine.core.network.dto

import com.example.shine.core.model.Workspace
import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceDto(
    val id: String,
    val name: String,
    val isActive: Boolean? = true,
    val subdomain: String? = null,
    val logo: FileDto? = null,
)

fun WorkspaceDto.toDomain(): Workspace = Workspace(
    id = id,
    name = name,
    isActive = isActive ?: true,
    subdomain = subdomain,
    logoUrl = logo?.signedUrl,
)
