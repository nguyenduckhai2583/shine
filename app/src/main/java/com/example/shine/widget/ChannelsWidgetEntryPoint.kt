package com.example.shine.widget

import com.example.shine.data.local.SessionLocalDataSource
import com.example.shine.domain.repository.ChannelRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChannelsWidgetEntryPoint {
    fun sessionLocalDataSource(): SessionLocalDataSource
    fun channelRepository(): ChannelRepository
}
