package com.example.shine.core.di

import android.content.Context
import androidx.room.Room
import com.example.shine.data.local.ShineDatabase
import com.example.shine.data.local.dao.WorkspaceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideShineDatabase(
        @ApplicationContext context: Context,
    ): ShineDatabase = Room.databaseBuilder(
        context,
        ShineDatabase::class.java,
        "shine_database",
    ).fallbackToDestructiveMigration(dropAllTables = true).build()

    @Provides
    fun provideWorkspaceDao(
        database: ShineDatabase,
    ): WorkspaceDao = database.workspaceDao()
}
