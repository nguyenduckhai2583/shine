package com.example.shine.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors

class RefreshChannelsActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ChannelsWidgetEntryPoint::class.java,
        )
        runCatching {
            entryPoint.channelRepository().getChannels()
        }
        ChannelsGlanceWidget().update(context, glanceId)
    }
}
