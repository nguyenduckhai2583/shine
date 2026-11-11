package com.example.shine.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.shine.core.model.Project

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val prefix: String? = null,
    val status: String? = null,
    val logoUrl: String? = null,
    val leaderName: String? = null,
)

fun ProjectEntity.toDomain(): Project = Project(
    id = id,
    name = name,
    prefix = prefix,
    status = status,
    logoUrl = logoUrl,
    leaderName = leaderName,
)

fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    name = name,
    prefix = prefix,
    status = status,
    logoUrl = logoUrl,
    leaderName = leaderName,
)
