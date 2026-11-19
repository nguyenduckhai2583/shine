package com.example.shine.widget

import com.example.shine.core.data.repository.ChannelRepository
import com.example.shine.core.datastore.SessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChannelsWidgetEntryPoint {
    fun sessionManager(): SessionManager
    fun channelRepository(): ChannelRepository
}
