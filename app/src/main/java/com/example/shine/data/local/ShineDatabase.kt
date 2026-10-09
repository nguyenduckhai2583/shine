package com.example.shine.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.shine.data.local.dao.WorkspaceDao
import com.example.shine.data.local.entity.WorkspaceEntity

@Database(entities = [WorkspaceEntity::class], version = 1, exportSchema = false)
abstract class ShineDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
}
