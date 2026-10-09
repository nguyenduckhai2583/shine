package com.example.shine.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.shine.data.repository.AuthRepositoryImpl
import com.example.shine.data.repository.ChannelRepositoryImpl
import com.example.shine.data.repository.PlanixRepositoryImpl
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.domain.repository.ChannelRepository
import com.example.shine.domain.repository.PlanixRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds
    abstract fun bindPlanixRepository(impl: PlanixRepositoryImpl): PlanixRepository

    companion object {
        @Provides
        @Singleton
        fun providePreferencesDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("session")
        }
    }
}
