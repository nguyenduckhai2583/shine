package com.example.shine.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.shine.core.designsystem.theme.MultiDevicePreviews
import com.example.shine.core.designsystem.theme.ShineTheme
import com.example.shine.core.model.Channel
import com.example.shine.core.model.DirectMessage
import com.example.shine.core.model.Workspace
import com.example.shine.feature.channel.ChannelDetailScreen
import com.example.shine.feature.channel.ChannelDetailUiState
import com.example.shine.feature.channel.ChannelsContent
import com.example.shine.feature.channel.ChannelUiState
import com.example.shine.feature.dm.DirectMessageDetailScreen
import com.example.shine.feature.dm.DirectMessageDetailUiState
import com.example.shine.feature.dm.DmsContent
import com.example.shine.feature.dm.DmsUiState
import com.example.shine.feature.home.HomeScreen
import com.example.shine.feature.home.HomeTab
import com.example.shine.feature.home.HomeUiState
import com.example.shine.feature.profile.ProfileScreen
import com.example.shine.feature.profile.ProfileUiState
import com.example.shine.feature.profile.UploadState

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MockListDetailNavDisplay(
    initialKeys: List<NavKey>,
    modifier: Modifier = Modifier,
    initialTab: HomeTab = HomeTab.Channels,
) {
    val backStack = rememberNavBackStack(*initialKeys.toTypedArray())
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    val mockChannels = listOf(
        Channel(
            id = "1",
            name = "general",
            isPrivate = false,
            isEncrypted = false,
            isDefault = true,
            categoryName = null,
            createdAt = null,
            lastActivityAt = null,
        ),
        Channel(
            id = "2",
            name = "random",
            isPrivate = false,
            isEncrypted = false,
            isDefault = false,
            categoryName = null,
            createdAt = null,
            lastActivityAt = null,
        ),
    )

    val mockDms = listOf(
        DirectMessage(
            id = "1",
            name = "Alice Smith",
            time = 1_779_780_643L,
            type = "direct",
            createdAt = 1_779_700_000L,
            lastActivityAt = 1_779_780_643L,
        ),
        DirectMessage(
            id = "2",
            name = "Bob Jones",
            time = 1_779_780_000L,
            type = "direct",
            createdAt = 1_779_600_000L,
            lastActivityAt = 1_779_780_000L,
        ),
    )

    var selectedTab by remember(initialTab) {
        mutableStateOf(initialTab)
    }

    val isTwoPane = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2()).maxHorizontalPartitions > 1
    val detailOnBack: (() -> Unit)? = if (isTwoPane) null else backStack::popBack

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.popBack() },
        sceneStrategies = listOf(listDetailStrategy),
        modifier = modifier.fillMaxSize(),
        entryProvider = entryProvider {
            entry<Home>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { EmptyDetailPlaceholder() },
                ),
            ) {
                HomeScreen(
                    uiState = HomeUiState(
                        displayName = "Jane Doe",
                        workspaces = listOf(
                            Workspace("1", "Acme Corp", true, "default"),
                        ),
                        selectedWorkspaceId = "1",
                    ),
                    selectedTab = selectedTab,
                    onSelectTab = { tab ->
                        if (selectedTab != tab) {
                            selectedTab = tab
                            backStack.popUntil(Home)
                        }
                    },
                    onPlanixClick = {},
                    onProfileClick = {
                        backStack.popUntil(Home)
                        backStack.add(Profile)
                    },
                    onSignOut = {},
                    onSelectWorkspace = {},
                    channelsContent = {
                        ChannelsContent(
                            uiState = ChannelUiState(channels = mockChannels),
                            onChannelClick = { channel ->
                                backStack.popUntil(Home)
                                backStack.add(ChannelDetail(channel.id))
                            },
                            onRefresh = {},
                        )
                    },
                    dmsContent = {
                        DmsContent(
                            uiState = DmsUiState(dms = mockDms),
                            onDmClick = { dm ->
                                backStack.popUntil(Home)
                                backStack.add(DirectMessageDetail(dm.id))
                            },
                            onRefresh = {},
                        )
                    },
                )
            }
            entry<ChannelDetail>(
                metadata = ListDetailSceneStrategy.detailPane(),
            ) { key ->
                val selectedChannel = mockChannels.find { it.id == key.channelId }
                    ?: Channel(
                        id = key.channelId,
                        name = "channel-${key.channelId}",
                        isPrivate = false,
                        isEncrypted = false,
                        isDefault = false,
                        categoryName = null,
                        createdAt = null,
                        lastActivityAt = null,
                    )
                ChannelDetailScreen(
                    uiState = ChannelDetailUiState.Success(channel = selectedChannel),
                    onBack = detailOnBack,
                )
            }
            entry<DirectMessageDetail>(
                metadata = ListDetailSceneStrategy.detailPane(),
            ) { key ->
                val selectedDm = mockDms.find { it.id == key.dmId }
                    ?: DirectMessage(
                        id = key.dmId,
                        name = "User ${key.dmId}",
                        time = null,
                        type = "direct",
                        createdAt = null,
                        lastActivityAt = null,
                    )
                DirectMessageDetailScreen(
                    uiState = DirectMessageDetailUiState.Success(directMessage = selectedDm),
                    onBack = detailOnBack,
                )
            }
            entry<Profile>(
                metadata = ListDetailSceneStrategy.detailPane(),
            ) {
                ProfileScreen(
                    uiState = ProfileUiState(
                        name = "Jane Doe",
                        avatarUrl = null,
                        uploadState = UploadState.Idle,
                    ),
                    onBack = detailOnBack,
                    onAvatarSelected = {},
                )
            }
        },
    )
}

/**
 * Preview khi vừa mở Home (chưa chọn channel nào):
 * - Phone: Chỉ thấy Home (danh sách channel)
 * - Tablet: Thấy 2 cột: Bên trái là Home, bên phải là Empty placeholder
 */
@MultiDevicePreviews
@Composable
private fun ListDetailInitialPreview() {
    ShineTheme(dynamicColor = false) {
        MockListDetailNavDisplay(initialKeys = listOf(Home))
    }
}

/**
 * Preview khi đã chọn Channel "1" (#general):
 * - Phone: Thấy Detail của #general full màn hình
 * - Tablet: Thấy 2 cột: Bên trái là Home, bên phải là Detail của #general
 */
@MultiDevicePreviews
@Composable
private fun ListDetailSelectedPreview() {
    ShineTheme(dynamicColor = false) {
        MockListDetailNavDisplay(initialKeys = listOf(Home, ChannelDetail("1")))
    }
}

/**
 * Preview khi vừa mở Home ở tab DMs (chưa chọn DM nào):
 * - Phone: Chỉ thấy Home (danh sách DM)
 * - Tablet: Thấy 2 cột: Bên trái là Home, bên phải là Empty DM placeholder
 */
@MultiDevicePreviews
@Composable
private fun ListDetailDmInitialPreview() {
    ShineTheme(dynamicColor = false) {
        MockListDetailNavDisplay(initialKeys = listOf(Home), initialTab = HomeTab.Dms)
    }
}

/**
 * Preview khi đã chọn Direct Message "1" (Alice Smith):
 * - Phone: Thấy Detail của Alice Smith full màn hình
 * - Tablet: Thấy 2 cột: Bên trái là Home, bên phải là Detail của Alice Smith
 */
@MultiDevicePreviews
@Composable
private fun ListDetailDmSelectedPreview() {
    ShineTheme(dynamicColor = false) {
        MockListDetailNavDisplay(
            initialKeys = listOf(Home, DirectMessageDetail("1")),
            initialTab = HomeTab.Dms,
        )
    }
}

/**
 * Preview khi mở Settings và chọn Profile:
 * - Phone: Thấy Profile full màn hình (có nút Back)
 * - Tablet: Thấy 2 cột: Bên trái là Settings, bên phải là Profile (không có nút Back)
 */
@MultiDevicePreviews
@Composable
private fun ListDetailProfileSelectedPreview() {
    ShineTheme(dynamicColor = false) {
        MockListDetailNavDisplay(
            initialKeys = listOf(Home, Profile),
            initialTab = HomeTab.Settings,
        )
    }
}
