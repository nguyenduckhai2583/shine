package com.example.shine.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.shine.domain.model.Workspace

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val isActive: Boolean = true,
    val subdomain: String? = null,
    val logoUrl: String? = null,
)

fun WorkspaceEntity.toDomain(): Workspace = Workspace(
    id = id,
    name = name,
    isActive = isActive,
    subdomain = subdomain,
    logoUrl = logoUrl,
)

fun Workspace.toEntity(): WorkspaceEntity = WorkspaceEntity(
    id = id,
    name = name,
    isActive = isActive,
    subdomain = subdomain,
    logoUrl = logoUrl,
)
