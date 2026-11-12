package com.example.shine.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.shine.R
import com.example.shine.feature.auth.SignInRoute
import com.example.shine.feature.camera.CameraCaptureMode
import com.example.shine.feature.camera.CameraRoute
import com.example.shine.feature.camera.CapturedMedia
import com.example.shine.feature.camera.MediaPreviewRoute
import com.example.shine.feature.channel.ChannelDetailRoute
import com.example.shine.feature.home.HomeRoute
import com.example.shine.feature.planix.PlanixListRoute
import com.example.shine.feature.profile.ProfileRoute
import com.example.shine.feature.workspace.WorkspaceListRoute
import com.example.shine.ui.navigation.deeplink.DeepLinkDestination

@Composable
fun ShineApp(viewModel: AppViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val pendingDestination by viewModel.pendingDestination.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()

    val backStack = rememberNavBackStack(SignIn)

    LaunchedEffect(authState) {
        when (authState) {
            AuthState.Initializing -> Unit
            AuthState.Unauthenticated -> backStack.resetTo(SignIn)
            AuthState.SelectWorkspace -> backStack.resetTo(WorkspaceList)
            AuthState.Authenticated -> backStack.resetTo(Home)
        }
    }

    LaunchedEffect(pendingDestination, authState) {
        val dest = pendingDestination
        if (authState == AuthState.Authenticated && dest != null) {
            when (dest) {
                is DeepLinkDestination.Channel -> {
                    val targetKey = ChannelDetail(dest.channelId)
                    if (backStack.lastOrNull() != targetKey) {
                        backStack.add(targetKey)
                    }
                    viewModel.onDeepLinkConsumed()
                }
                is DeepLinkDestination.Profile -> {
                    backStack.removeAll { it !is Home }
                    backStack.add(Profile)
                    viewModel.onDeepLinkConsumed()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (authState != AuthState.Initializing) {
            NavDisplay(
                backStack = backStack,
                onBack = backStack::popBack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                    rememberResultEventBusNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<SignIn> {
                        SignInRoute()
                    }
                    entry<WorkspaceList> {
                        WorkspaceListRoute()
                    }
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
                                backStack.add(CameraCapture(resultKey = "profile_avatar"))
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

        AnimatedVisibility(
            visible = isOffline,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                shadowElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.error_no_internet),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

private fun MutableList<NavKey>.resetTo(key: NavKey) {
    if (key !in this) {
        clear()
        add(key)
    }
}

private fun MutableList<NavKey>.popBack() {
    if (size > 1) removeLastOrNull()
}

