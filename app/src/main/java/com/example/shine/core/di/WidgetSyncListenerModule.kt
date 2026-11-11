package com.example.shine.core.di

import com.example.shine.core.data.sync.ChannelSyncListener
import com.example.shine.widget.ChannelsWidgetUpdater
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object WidgetSyncListenerModule {

    @Provides
    @IntoSet
    fun provideWidgetChannelSyncListener(): ChannelSyncListener = ChannelSyncListener { context ->
        ChannelsWidgetUpdater.update(context)
    }
}
