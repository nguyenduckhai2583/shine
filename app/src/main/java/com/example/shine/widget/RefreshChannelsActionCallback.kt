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
        val workspaceId = entryPoint.sessionManager().getSession()?.workspaceId
        if (workspaceId != null) {
            runCatching {
                entryPoint.channelRepository().getChannels(workspaceId)
            }
        }
        ChannelsGlanceWidget().update(context, glanceId)
    }
}
