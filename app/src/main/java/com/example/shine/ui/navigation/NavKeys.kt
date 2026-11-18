package com.example.shine.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.example.shine.feature.camera.CameraCaptureMode
import com.example.shine.feature.camera.CapturedMedia
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
data class PlanixDetail(val projectId: String) : NavKey

@Serializable
data object WorkspaceList : NavKey

@Serializable
data object Profile : NavKey

@Serializable
data class CameraCapture(
    val resultKey: String,
    val modes: List<CameraCaptureMode> = listOf(CameraCaptureMode.PHOTO),
) : NavKey

@Serializable
data class CaptureMediaPreview(
    val media: CapturedMedia,
    val resultKey: String = "",
) : NavKey
