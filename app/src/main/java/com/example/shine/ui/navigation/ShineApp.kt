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
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.shine.feature.channel.ChannelsRoute
import com.example.shine.feature.dm.DirectMessageDetailRoute
import com.example.shine.feature.dm.DmsRoute
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (authState != AuthState.Initializing) {
            ShineNavDisplay(
                authState = authState,
                pendingDestination = pendingDestination,
                onDeepLinkConsumed = viewModel::onDeepLinkConsumed,
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ShineNavDisplay(
    authState: AuthState,
    pendingDestination: DeepLinkDestination?,
    onDeepLinkConsumed: () -> Unit,
) {
    val initialKey = remember {
        when (authState) {
            AuthState.Authenticated -> Home
            AuthState.SelectWorkspace -> WorkspaceList
            else -> SignIn
        }
    }
    val backStack = rememberNavBackStack(initialKey)

    LaunchedEffect(authState) {
        when (authState) {
            AuthState.Initializing -> Unit
            AuthState.Unauthenticated -> backStack.resetTo(SignIn)
            AuthState.SelectWorkspace -> backStack.resetTo(WorkspaceList)
            AuthState.Authenticated -> backStack.resetTo(Home)
        }
    }

    LaunchedEffect(pendingDestination, authState) {
        if (authState == AuthState.Authenticated && pendingDestination != null) {
            when (pendingDestination) {
                is DeepLinkDestination.Channel -> {
                    val targetKey = ChannelDetail(pendingDestination.channelId)
                    if (backStack.lastOrNull() != targetKey) {
                        backStack.popUntil(Home)
                        backStack.add(targetKey)
                    }
                    onDeepLinkConsumed()
                }

                is DeepLinkDestination.Profile -> {
                    backStack.popUntil(Home)
                    backStack.add(Profile)
                    onDeepLinkConsumed()
                }
            }
        }
    }

    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    NavDisplay(
        backStack = backStack,
        onBack = backStack::popBack,
        sceneStrategies = listOf(listDetailStrategy),
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
            entry<Home>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        EmptyDetailPlaceholder()
                    },
                ),
            ) {
                HomeRoute(
                    channelsContent = {
                        ChannelsRoute(
                            onChannelClick = { channel ->
                                backStack.popUntil(Home)
                                backStack.add(ChannelDetail(channel.id))
                            },
                        )
                    },
                    dmsContent = {
                        DmsRoute(
                            onDmClick = { dm ->
                                backStack.popUntil(Home)
                                backStack.add(DirectMessageDetail(dm.id))
                            },
                        )
                    },
                    onPlanixClick = { backStack.add(PlanixList) },
                    onProfileClick = { backStack.add(Profile) },
                )
            }
            entry<ChannelDetail>(
                metadata = ListDetailSceneStrategy.detailPane(),
            ) { key ->
                ChannelDetailRoute(
                    channelId = key.channelId,
                    onBack = { backStack.popBack() },
                )
            }
            entry<DirectMessageDetail>(
                metadata = ListDetailSceneStrategy.detailPane(),
            ) { key ->
                DirectMessageDetailRoute(
                    dmId = key.dmId,
                    onBack = { backStack.popBack() },
                )
            }
            entry<PlanixList> {
                PlanixListRoute(
                    onBack = { backStack.popBack() },
                )
            }
            entry<Profile> {
                ProfileRoute(
                    onBack = { backStack.popBack() },
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
                    onClose = { backStack.popBack() },
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

internal fun MutableList<NavKey>.popUntil(key: NavKey) {
    if (key !in this) return
    while (isNotEmpty() && last() != key) {
        removeAt(lastIndex)
    }
}

internal fun MutableList<NavKey>.resetTo(key: NavKey) {
    clear()
    add(key)
}

internal fun MutableList<NavKey>.popBack() {
    if (size > 1) removeLastOrNull()
}

