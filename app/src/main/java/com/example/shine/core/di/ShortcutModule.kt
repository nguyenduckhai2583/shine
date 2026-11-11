package com.example.shine.core.di

import com.example.shine.core.shortcut.AppShortcutManager
import com.example.shine.core.shortcut.AppShortcutManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ShortcutModule {

    @Binds
    abstract fun bindAppShortcutManager(impl: AppShortcutManagerImpl): AppShortcutManager
}
