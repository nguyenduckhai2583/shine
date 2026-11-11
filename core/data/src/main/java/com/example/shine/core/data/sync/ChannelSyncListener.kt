package com.example.shine.core.data.sync

import android.content.Context

fun interface ChannelSyncListener {
    suspend fun onChannelsSynced(context: Context)
}
