package com.example.shine.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.example.shine.feature.conversationsettings.ConversationType
import kotlinx.serialization.Serializable

@Serializable
data object SignIn : NavKey

@Serializable
data object Home : NavKey

@Serializable
data class ChannelDetail(val channelId: String) : NavKey

@Serializable
data class ConversationSettings(
    val conversationId: String,
    val type: ConversationType,
) : NavKey

@Serializable
data class DirectMessageDetail(val dmId: String) : NavKey

@Serializable
data object PlanixList : NavKey

@Serializable
data object WorkspaceList : NavKey

@Serializable
data object Profile : NavKey

@Serializable
data class CameraCapture(
    val resultKey: String,
    val allowVideo: Boolean = false,
) : NavKey

@Serializable
data class MediaPreview(
    val uri: String,
    val isVideo: Boolean = false,
    val durationMs: Long = 0L,
    val resultKey: String = "",
) : NavKey

