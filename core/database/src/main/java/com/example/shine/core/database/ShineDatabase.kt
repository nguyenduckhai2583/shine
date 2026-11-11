package com.example.shine.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.shine.core.database.dao.ChannelDao
import com.example.shine.core.database.dao.ProjectDao
import com.example.shine.core.database.dao.ProjectRemoteKeyDao
import com.example.shine.core.database.dao.UserDao
import com.example.shine.core.database.dao.WorkspaceDao
import com.example.shine.core.database.entity.ChannelEntity
import com.example.shine.core.database.entity.ProjectEntity
import com.example.shine.core.database.entity.ProjectRemoteKeyEntity
import com.example.shine.core.database.entity.UserEntity
import com.example.shine.core.database.entity.WorkspaceEntity

@Database(
    entities = [
        WorkspaceEntity::class,
        ProjectEntity::class,
        ProjectRemoteKeyEntity::class,
        UserEntity::class,
        ChannelEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class ShineDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectRemoteKeyDao(): ProjectRemoteKeyDao
    abstract fun userDao(): UserDao
    abstract fun channelDao(): ChannelDao

    open suspend fun <R> runTransaction(block: suspend () -> R): R =
        withTransaction(block)
}
