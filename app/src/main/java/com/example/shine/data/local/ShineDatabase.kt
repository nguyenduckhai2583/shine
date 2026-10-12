package com.example.shine.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.shine.data.local.dao.ProjectDao
import com.example.shine.data.local.dao.ProjectRemoteKeyDao
import com.example.shine.data.local.dao.WorkspaceDao
import com.example.shine.data.local.entity.ProjectEntity
import com.example.shine.data.local.entity.ProjectRemoteKeyEntity
import com.example.shine.data.local.entity.WorkspaceEntity

@Database(
    entities = [
        WorkspaceEntity::class,
        ProjectEntity::class,
        ProjectRemoteKeyEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class ShineDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectRemoteKeyDao(): ProjectRemoteKeyDao

    open suspend fun <R> runTransaction(block: suspend () -> R): R =
        withTransaction(block)
}
