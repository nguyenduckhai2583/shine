package com.example.shine.core.di

import com.example.shine.BuildConfig
import com.example.shine.data.remote.socket.ConversationSocketMediator
import com.example.shine.data.remote.socket.SocketEventDeduplicator
import com.example.shine.data.remote.socket.SocketEventDeduplicatorImpl
import com.example.shine.data.remote.socket.SocketMediator
import com.example.shine.data.remote.socket.SocketService
import com.example.shine.data.remote.socket.SocketServiceImpl
import com.example.shine.data.remote.socket.client.SocketClient
import com.example.shine.data.remote.socket.client.SocketIoClientImpl
import com.example.shine.core.util.AppLifecycleMonitor
import com.example.shine.core.util.ConnectivityManagerNetworkMonitor
import com.example.shine.core.util.NetworkMonitor
import com.example.shine.core.util.ProcessAppLifecycleMonitor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
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

    @Binds
    @IntoSet
    abstract fun bindConversationSocketMediator(impl: ConversationSocketMediator): SocketMediator

    companion object {
        @Provides
        @Singleton
        fun provideSocketClient(impl: SocketIoClientImpl): SocketClient {
            impl.init(BuildConfig.BASE_URL)
            return impl
        }
    }
}
