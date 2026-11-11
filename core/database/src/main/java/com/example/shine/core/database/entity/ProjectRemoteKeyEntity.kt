package com.example.shine.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_remote_keys")
data class ProjectRemoteKeyEntity(
    @PrimaryKey
    val projectId: String,
    val prevKey: Int?,
    val nextKey: Int?,
)
