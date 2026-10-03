package com.example.shine.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.shine.data.local.dao.ChannelDao
import com.example.shine.data.local.entity.ChannelEntity

@Database(
    entities = [ChannelEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ShineDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
}
