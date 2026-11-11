package com.example.shine.core.data.di

import com.example.shine.core.data.repository.AuthRepository
import com.example.shine.core.data.repository.AuthRepositoryImpl
import com.example.shine.core.data.repository.ChannelRepository
import com.example.shine.core.data.repository.ChannelRepositoryImpl
import com.example.shine.core.data.repository.DirectMessageRepository
import com.example.shine.core.data.repository.DirectMessageRepositoryImpl
import com.example.shine.core.data.repository.PlanixRepository
import com.example.shine.core.data.repository.PlanixRepositoryImpl
import com.example.shine.core.data.repository.UserRepository
import com.example.shine.core.data.repository.UserRepositoryImpl
import com.example.shine.core.data.repository.WorkspaceRepository
import com.example.shine.core.data.repository.WorkspaceRepositoryImpl
import com.example.shine.core.data.socket.ConversationSocketMediator
import com.example.shine.core.data.sync.ChannelSyncListener
import com.example.shine.core.network.socket.SocketMediator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
abstract class DataModule {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds
    abstract fun bindDirectMessageRepository(impl: DirectMessageRepositoryImpl): DirectMessageRepository

    @Binds
    abstract fun bindPlanixRepository(impl: PlanixRepositoryImpl): PlanixRepository

    @Binds
    abstract fun bindWorkspaceRepository(impl: WorkspaceRepositoryImpl): WorkspaceRepository

    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @IntoSet
    abstract fun bindConversationSocketMediator(impl: ConversationSocketMediator): SocketMediator

    @Multibinds
    abstract fun bindChannelSyncListeners(): Set<ChannelSyncListener>
}
