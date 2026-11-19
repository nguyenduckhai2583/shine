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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
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
import com.example.shine.core.model.AuthState
import com.example.shine.feature.auth.SignInRoute
import com.example.shine.feature.auth.SignInViewModel
import com.example.shine.feature.camera.CameraRoute
import com.example.shine.feature.camera.CameraViewModel
import com.example.shine.feature.camera.CaptureMediaPreviewRoute
import com.example.shine.feature.channel.ChannelDetailRoute
import com.example.shine.feature.channel.ChannelDetailViewModel
import com.example.shine.feature.channel.ChannelsRoute
import com.example.shine.feature.conversationsettings.ConversationSettingsRoute
import com.example.shine.feature.conversationsettings.ConversationSettingsViewModel
import com.example.shine.feature.conversationsettings.ConversationType
import com.example.shine.feature.dm.DirectMessageDetailRoute
import com.example.shine.feature.dm.DirectMessageDetailViewModel
import com.example.shine.feature.dm.DmsRoute
import com.example.shine.feature.home.HomeRoute
import com.example.shine.feature.home.HomeViewModel
import com.example.shine.feature.home.components.HomeDetailPlaceholder
import com.example.shine.feature.planix.PlanixDetailPlaceholder
import com.example.shine.feature.planix.PlanixDetailRoute
import com.example.shine.feature.planix.PlanixDetailViewModel
import com.example.shine.feature.planix.PlanixListRoute
import com.example.shine.feature.planix.PlanixListViewModel
import com.example.shine.feature.profile.ProfileRoute
import com.example.shine.feature.profile.ProfileViewModel
import com.example.shine.feature.workspace.WorkspaceListRoute
import com.example.shine.feature.workspace.WorkspaceListViewModel
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
    val navigator = remember(backStack) {
        AppNavigator(
            backStack = backStack,
            authState = { authState },
        )
    }

    LaunchedEffect(authState) {
        when (authState) {
            AuthState.Unauthenticated -> navigator.resetTo(SignIn)
            AuthState.SelectWorkspace -> navigator.resetTo(WorkspaceList)
            AuthState.Authenticated -> {
                if (navigator.currentKey !is AuthenticatedNavKey) {
                    navigator.resetTo(Home)
                }
            }
            AuthState.Initializing -> Unit
        }
    }

    LaunchedEffect(pendingDestination, authState) {
        if (authState == AuthState.Authenticated && pendingDestination != null) {
            when (pendingDestination) {
                is DeepLinkDestination.Channel -> {
                    val targetKey = ChannelDetail(pendingDestination.channelId)
                    if (navigator.currentKey != targetKey) {
                        navigator.popUntil(Home)
                        navigator.navigate(targetKey)
                    }
                    onDeepLinkConsumed()
                }

                is DeepLinkDestination.Profile -> {
                    navigator.popUntil(Home)
                    navigator.navigate(Profile)
                    onDeepLinkConsumed()
                }
            }
        }
    }

    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    val isTwoPane =
        calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2()).maxHorizontalPartitions > 1
    val detailOnBack: (() -> Unit)? = if (isTwoPane) null else { { navigator.pop() } }

    val selectedChannelId =
        (navigator.backStack.lastOrNull { it is ChannelDetail } as? ChannelDetail)?.channelId
    val selectedDmId =
        (navigator.backStack.lastOrNull { it is DirectMessageDetail } as? DirectMessageDetail)?.dmId
    val selectedProjectId =
        (navigator.backStack.lastOrNull { it is PlanixDetail } as? PlanixDetail)?.projectId

    NavDisplay(
        backStack = navigator.backStack,
        onBack = { navigator.pop() },
        sceneStrategies = listOf(listDetailStrategy),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            rememberResultEventBusNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<SignIn> {
                val viewModel = hiltViewModel<SignInViewModel>()
                SignInRoute(viewModel = viewModel)
            }
            entry<WorkspaceList> {
                val viewModel = hiltViewModel<WorkspaceListViewModel>()
                WorkspaceListRoute(viewModel = viewModel)
            }
            entry<Home>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        HomeDetailPlaceholder()
                    },
                ),
            ) {
                val viewModel = hiltViewModel<HomeViewModel>()
                HomeRoute(
                    viewModel = viewModel,
                    channelsContent = {
                        ChannelsRoute(
                            selectedChannelId = selectedChannelId,
                            onChannelClick = { channel ->
                                navigator.popUntil(Home)
                                navigator.navigate(ChannelDetail(channel.id))
                            },
                        )
                    },
                    dmsContent = {
                        DmsRoute(
                            selectedDmId = selectedDmId,
                            onDmClick = { dm ->
                                navigator.popUntil(Home)
                                navigator.navigate(DirectMessageDetail(dm.id))
                            },
                        )
                    },
                    onPlanixClick = {
                        navigator.popUntil(Home)
                        navigator.navigate(PlanixList)
                    },
                    onProfileClick = {
                        navigator.popUntil(Home)
                        navigator.navigate(Profile)
                    },
                    onTabSelected = {
                        navigator.popUntil(Home)
                    },
                )
            }
            entry<ChannelDetail>(
                metadata = ListDetailSceneStrategy.detailPane(),
            ) { key ->
                val viewModel =
                    hiltViewModel<ChannelDetailViewModel, ChannelDetailViewModel.Factory>(
                        creationCallback = { factory -> factory.create(key.channelId) },
                    )
                ChannelDetailRoute(
                    viewModel = viewModel,
                    onBack = detailOnBack,
                    onNavigateToSettings = {
                        navigator.navigate(ConversationSettings(key.channelId, ConversationType.CHANNEL))
                    },
                )
            }
            entry<DirectMessageDetail>(
                metadata = ListDetailSceneStrategy.detailPane(),
            ) { key ->
                val viewModel =
                    hiltViewModel<DirectMessageDetailViewModel, DirectMessageDetailViewModel.Factory>(
                        creationCallback = { factory -> factory.create(key.dmId) },
                    )
                DirectMessageDetailRoute(
                    viewModel = viewModel,
                    onBack = detailOnBack,
                    onNavigateToSettings = {
                        navigator.navigate(ConversationSettings(key.dmId, ConversationType.DM))
                    },
                )
            }
            entry<ConversationSettings>(
                metadata = ListDetailSceneStrategy.detailPane(),
            ) { key ->
                val viewModel =
                    hiltViewModel<ConversationSettingsViewModel, ConversationSettingsViewModel.Factory>(
                        creationCallback = { factory ->
                            factory.create(
                                key.conversationId,
                                key.type
                            )
                        },
                    )
                ConversationSettingsRoute(
                    viewModel = viewModel,
                    onBack = { navigator.pop() },
                )
            }
            entry<PlanixList>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { PlanixDetailPlaceholder() },
                ),
            ) {
                val viewModel = hiltViewModel<PlanixListViewModel>()
                PlanixListRoute(
                    viewModel = viewModel,
                    selectedProjectId = selectedProjectId,
                    onBack = { navigator.popUntil(Home) },
                    onProjectClick = { project ->
                        navigator.popUntil(PlanixList)
                        navigator.navigate(PlanixDetail(project.id))
                    },
                )
            }
            entry<PlanixDetail>(
                metadata = ListDetailSceneStrategy.detailPane(),
            ) { key ->
                val viewModel = hiltViewModel<PlanixDetailViewModel, PlanixDetailViewModel.Factory>(
                    creationCallback = { factory -> factory.create(key.projectId) },
                )
                PlanixDetailRoute(
                    viewModel = viewModel,
                    onBack = detailOnBack,
                )
            }
            entry<Profile>(
                metadata = ListDetailSceneStrategy.detailPane(),
            ) {
                val viewModel = hiltViewModel<ProfileViewModel>()
                ProfileRoute(
                    viewModel = viewModel,
                    onBack = detailOnBack,
                    onOpenCamera = {
                        navigator.navigate(CameraCapture(resultKey = "profile_avatar"))
                    },
                )
            }
            entry<CameraCapture> { key ->
                val viewModel = hiltViewModel<CameraViewModel>()
                CameraRoute(
                    viewModel = viewModel,
                    allowedModes = key.modes.toSet(),
                    onMediaCaptured = { media ->
                        navigator.navigate(
                            CaptureMediaPreview(
                                media = media,
                                resultKey = key.resultKey,
                            )
                        )
                    },
                    onClose = { navigator.pop() },
                )
            }
            entry<CaptureMediaPreview> { key ->
                val resultBus = LocalResultEventBus.current
                CaptureMediaPreviewRoute(
                    media = key.media,
                    onRetake = { navigator.pop() },
                    onConfirm = { confirmedMedia ->
                        if (key.resultKey.isNotBlank()) {
                            resultBus.sendResult(key.resultKey, confirmedMedia.uri)
                        }
                        navigator.removeAll { it is CaptureMediaPreview || it is CameraCapture }
                    },
                )
            }
        },
    )
}
