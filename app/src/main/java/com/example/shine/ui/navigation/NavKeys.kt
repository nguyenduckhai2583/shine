package com.example.shine.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.example.shine.feature.camera.CameraCaptureMode
import com.example.shine.feature.camera.CapturedMedia
import com.example.shine.feature.conversationsettings.ConversationType
import kotlinx.serialization.Serializable

/**
 * Navigation keys only accessible when unauthenticated.
 */
sealed interface UnauthenticatedNavKey : NavKey

/**
 * Navigation keys only accessible during workspace selection.
 */
sealed interface SelectWorkspaceNavKey : NavKey

/**
 * Navigation keys only accessible within an authenticated workspace session.
 */
sealed interface AuthenticatedNavKey : NavKey

@Serializable
data object SignIn : UnauthenticatedNavKey

@Serializable
data object WorkspaceList : SelectWorkspaceNavKey

@Serializable
data object Home : AuthenticatedNavKey

@Serializable
data class ChannelDetail(val channelId: String) : AuthenticatedNavKey

@Serializable
data class ConversationSettings(
    val conversationId: String,
    val type: ConversationType,
) : AuthenticatedNavKey

@Serializable
data class DirectMessageDetail(val dmId: String) : AuthenticatedNavKey

@Serializable
data object PlanixList : AuthenticatedNavKey

@Serializable
data class PlanixDetail(val projectId: String) : AuthenticatedNavKey

@Serializable
data object Profile : AuthenticatedNavKey

@Serializable
data class CameraCapture(
    val resultKey: String,
    val modes: List<CameraCaptureMode> = listOf(CameraCaptureMode.PHOTO),
) : AuthenticatedNavKey

@Serializable
data class CaptureMediaPreview(
    val media: CapturedMedia,
    val resultKey: String = "",
) : AuthenticatedNavKey
