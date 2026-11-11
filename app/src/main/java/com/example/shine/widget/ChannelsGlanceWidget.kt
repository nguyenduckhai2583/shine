package com.example.shine.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.shine.BuildConfig
import com.example.shine.MainActivity
import com.example.shine.core.model.Channel
import com.example.shine.core.model.isSignedIn
import dagger.hilt.android.EntryPointAccessors
import androidx.core.net.toUri
import kotlinx.coroutines.flow.firstOrNull

class ChannelsGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ChannelsWidgetEntryPoint::class.java,
        )

        val initialSession = entryPoint.sessionLocalDataSource().session.firstOrNull()
        val initialIsSignedIn = initialSession.isSignedIn
        val initialChannels = if (initialIsSignedIn) {
            entryPoint.channelRepository().getRecentChannels(limit = 3)
        } else {
            emptyList()
        }

        provideContent {
            val session by entryPoint.sessionLocalDataSource().session.collectAsState(initial = initialSession)
            val isSignedIn = session.isSignedIn

            val channels by entryPoint.channelRepository()
                .getChannelsFlow(limit = 3)
                .collectAsState(initial = initialChannels)

            GlanceTheme {
                WidgetRoot(
                    context = context,
                    isSignedIn = isSignedIn,
                    channels = if (isSignedIn) channels else emptyList(),
                )
            }
        }
    }

    @Composable
    private fun WidgetRoot(
        context: Context,
        isSignedIn: Boolean,
        channels: List<Channel>,
    ) {
        if (!isSignedIn) {
            UnauthenticatedContent(context = context)
        } else {
            ChannelsListContent(context = context, channels = channels)
        }
    }

    @Composable
    private fun UnauthenticatedContent(context: Context) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(16.dp)
                .clickable(actionStartActivity(createMainIntent(context))),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Welcome Shine",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Tap to sign in",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                )
            }
        }
    }

    @Composable
    private fun ChannelsListContent(
        context: Context,
        channels: List<Channel>,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(12.dp),
        ) {
            // Header
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Channels",
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity(createMainIntent(context))),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = "↻",
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<RefreshChannelsActionCallback>())
                        .padding(horizontal = 4.dp),
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            if (channels.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity(createMainIntent(context))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No channels yet",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                        ),
                    )
                }
            } else {
                Column(
                    modifier = GlanceModifier.fillMaxWidth(),
                ) {
                    channels.forEach { channel ->
                        ChannelItemRow(
                            context = context,
                            channel = channel,
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun ChannelItemRow(
        context: Context,
        channel: Channel,
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(8.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .clickable(actionStartActivity(createChannelIntent(context, channel.id))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (channel.isPrivate) "🔒" else "#",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = channel.name,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }

    private fun createChannelIntent(context: Context, channelId: String): Intent {
        return Intent(
            Intent.ACTION_VIEW,
            "https://${BuildConfig.DEEP_LINK_HOST}/chat/channels/$channelId".toUri(),
            context,
            MainActivity::class.java,
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    private fun createMainIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
}
