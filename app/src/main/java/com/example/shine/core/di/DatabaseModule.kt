package com.example.shine.core.di

import android.content.Context
import androidx.room.Room
import com.example.shine.data.local.ShineDatabase
import com.example.shine.data.local.dao.ProjectDao
import com.example.shine.data.local.dao.ProjectRemoteKeyDao
import com.example.shine.data.local.dao.UserDao
import com.example.shine.data.local.dao.WorkspaceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalDatabase

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class InMemoryDatabase

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    @LocalDatabase
    fun provideLocalShineDatabase(
        @ApplicationContext context: Context,
    ): ShineDatabase = Room.databaseBuilder(
        context,
        ShineDatabase::class.java,
        "shine_database",
    ).fallbackToDestructiveMigration(dropAllTables = true).build()

    @Provides
    @Singleton
    @InMemoryDatabase
    fun provideInMemoryShineDatabase(
        @ApplicationContext context: Context,
    ): ShineDatabase = Room.inMemoryDatabaseBuilder(
        context,
        ShineDatabase::class.java,
    ).build()

    @Provides
    @Singleton
    fun provideShineDatabase(
        @LocalDatabase database: ShineDatabase,
    ): ShineDatabase = database

    @Provides
    fun provideWorkspaceDao(
        @LocalDatabase database: ShineDatabase,
    ): WorkspaceDao = database.workspaceDao()

    @Provides
    fun provideUserDao(
        @LocalDatabase database: ShineDatabase,
    ): UserDao = database.userDao()

    @Provides
    fun provideProjectDao(
        @InMemoryDatabase database: ShineDatabase,
    ): ProjectDao = database.projectDao()

    @Provides
    fun provideProjectRemoteKeyDao(
        @InMemoryDatabase database: ShineDatabase,
    ): ProjectRemoteKeyDao = database.projectRemoteKeyDao()
}
