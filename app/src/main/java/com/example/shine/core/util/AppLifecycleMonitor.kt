package com.example.shine.core.util

import kotlinx.coroutines.flow.Flow

interface AppLifecycleMonitor {
    val isInForeground: Flow<Boolean>
}
