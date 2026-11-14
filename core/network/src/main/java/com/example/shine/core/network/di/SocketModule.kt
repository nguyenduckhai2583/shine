package com.example.shine.core.network.di

import com.example.shine.core.common.network.ConnectivityManagerNetworkMonitor
import com.example.shine.core.common.network.NetworkMonitor
import com.example.shine.core.common.util.AppLifecycleMonitor
import com.example.shine.core.common.util.ProcessAppLifecycleMonitor
import com.example.shine.core.network.BuildConfig
import com.example.shine.core.network.socket.SocketEventDeduplicator
import com.example.shine.core.network.socket.SocketEventDeduplicatorImpl
import com.example.shine.core.network.socket.SocketService
import com.example.shine.core.network.socket.SocketServiceImpl
import com.example.shine.core.network.socket.client.SocketClient
import com.example.shine.core.network.socket.client.SocketIoClientImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SocketModule {

    @Binds
    abstract fun bindNetworkMonitor(impl: ConnectivityManagerNetworkMonitor): NetworkMonitor

    @Binds
    abstract fun bindAppLifecycleMonitor(impl: ProcessAppLifecycleMonitor): AppLifecycleMonitor

    @Binds
    abstract fun bindSocketService(impl: SocketServiceImpl): SocketService

    @Binds
    abstract fun bindSocketEventDeduplicator(impl: SocketEventDeduplicatorImpl): SocketEventDeduplicator

    companion object {
        @Provides
        @Singleton
        fun provideSocketClient(impl: SocketIoClientImpl): SocketClient {
            impl.init(BuildConfig.BASE_URL)
            return impl
        }
    }
}
