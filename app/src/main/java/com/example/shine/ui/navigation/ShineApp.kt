package com.example.shine.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.compose.runtime.LaunchedEffect
import com.example.shine.ui.navigation.deeplink.DeepLinkDestination
import com.example.shine.ui.channel.ChannelDetailRoute
import com.example.shine.ui.home.HomeRoute
import com.example.shine.ui.planix.PlanixListRoute
import com.example.shine.ui.profile.ProfileRoute
import com.example.shine.ui.signin.SignInRoute
import com.example.shine.ui.workspace.WorkspaceListRoute
import com.example.shine.ui.camera.CameraCaptureMode
import com.example.shine.ui.camera.CameraRoute
import com.example.shine.ui.camera.CapturedMedia
import com.example.shine.ui.camera.MediaPreviewRoute
import androidx.core.net.toUri

/**
 * Login is mandatory, so unauthenticated and authenticated flows are separate back stacks.
 * A successful sign-in persists the session. If user has multiple workspaces,
 * [AppViewModel] transitions to [AuthState.SelectWorkspace]. Once selected,
 * it transitions to [AuthState.Authenticated] (Home).
 */
@Composable
fun ShineApp(viewModel: AppViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val pendingDestination by viewModel.pendingDestination.collectAsStateWithLifecycle()

    when (authState) {
        AuthState.Initializing -> {}
        AuthState.Unauthenticated -> AuthNavigation()
        AuthState.SelectWorkspace -> WorkspaceNavigation()
        AuthState.Authenticated -> MainNavigation(
            pendingDestination = pendingDestination,
            onDestinationConsumed = viewModel::onDeepLinkConsumed,
        )
    }
}

@Composable
private fun AuthNavigation() {
    val backStack = rememberNavBackStack(SignIn)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<SignIn> { SignInRoute() }
        },
    )
}

@Composable
private fun WorkspaceNavigation() {
    val backStack = rememberNavBackStack(WorkspaceList)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<WorkspaceList> { WorkspaceListRoute() }
        },
    )
}

@Composable
private fun MainNavigation(
    pendingDestination: DeepLinkDestination?,
    onDestinationConsumed: () -> Unit,
) {
    val backStack = rememberNavBackStack(Home)

    LaunchedEffect(pendingDestination) {
        when (val destination = pendingDestination) {
            is DeepLinkDestination.Channel -> {
                val targetKey = ChannelDetail(destination.channelId)
                if (backStack.lastOrNull() != targetKey) {
                    backStack.add(targetKey)
                }
                onDestinationConsumed()
            }
            null -> Unit
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            rememberResultEventBusNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Home> {
                HomeRoute(
                    onChannelClick = { channel -> backStack.add(ChannelDetail(channel.id)) },
                    onPlanixClick = { backStack.add(PlanixList) },
                    onProfileClick = { backStack.add(Profile) },
                )
            }
            entry<ChannelDetail> { key ->
                ChannelDetailRoute(
                    channelId = key.channelId,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
            entry<PlanixList> {
                PlanixListRoute(
                    onBack = { backStack.removeLastOrNull() },
                )
            }
            entry<Profile> {
                ProfileRoute(
                    onBack = { backStack.removeLastOrNull() },
                    onOpenCamera = {
                        backStack.add(CameraCapture(resultKey = "profile_avatar", allowVideo = true))
                    },
                )
            }
            entry<CameraCapture> { key ->
                val allowedModes = if (key.allowVideo) {
                    setOf(CameraCaptureMode.PHOTO, CameraCaptureMode.VIDEO)
                } else {
                    setOf(CameraCaptureMode.PHOTO)
                }
                CameraRoute(
                    allowedModes = allowedModes,
                    onMediaCaptured = { media ->
                        val isVideo = media is CapturedMedia.Video
                        val durationMs = (media as? CapturedMedia.Video)?.durationMs ?: 0L
                        backStack.add(
                            MediaPreview(
                                uri = media.uri.toString(),
                                isVideo = isVideo,
                                durationMs = durationMs,
                                resultKey = key.resultKey,
                            )
                        )
                    },
                    onClose = { backStack.removeLastOrNull() },
                )
            }
            entry<MediaPreview> { key ->
                val resultBus = LocalResultEventBus.current
                val media = if (key.isVideo) {
                    CapturedMedia.Video(key.uri.toUri(), key.durationMs)
                } else {
                    CapturedMedia.Photo(key.uri.toUri())
                }
                MediaPreviewRoute(
                    media = media,
                    onRetake = { backStack.removeLastOrNull() },
                    onConfirm = { confirmedMedia ->
                        if (key.resultKey.isNotBlank()) {
                            resultBus.sendResult(key.resultKey, confirmedMedia.uri.toString())
                        }
                        backStack.removeAll { it is MediaPreview || it is CameraCapture }
                    },
                    onClose = {
                        backStack.removeAll { it is MediaPreview || it is CameraCapture }
                    },
                )
            }
        },
    )
}
