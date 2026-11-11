package com.example.shine.core.common.util

import kotlinx.coroutines.flow.Flow

interface AppLifecycleMonitor {
    val isInForeground: Flow<Boolean>
}
