package com.example.shine.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object ChannelsWidgetUpdater {
    suspend fun update(context: Context) {
        runCatching {
            ChannelsGlanceWidget().updateAll(context)
        }
    }
}
