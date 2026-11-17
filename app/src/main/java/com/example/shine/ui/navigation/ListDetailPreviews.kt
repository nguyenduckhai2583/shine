package com.example.shine.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.shine.core.designsystem.theme.MultiDevicePreviews
import com.example.shine.core.designsystem.theme.ShineTheme
import com.example.shine.core.model.Channel
import com.example.shine.core.model.Workspace
import com.example.shine.feature.channel.ChannelDetailScreen
import com.example.shine.feature.channel.ChannelDetailUiState
import com.example.shine.feature.channel.ChannelsContent
import com.example.shine.feature.channel.ChannelUiState
import com.example.shine.feature.dm.DmsContent
import com.example.shine.feature.dm.DmsUiState
import com.example.shine.feature.home.HomeScreen
import com.example.shine.feature.home.HomeTab
import com.example.shine.feature.home.HomeUiState

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MockListDetailNavDisplay(
    initialKeys: List<NavKey>,
    modifier: Modifier = Modifier,
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

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        sceneStrategies = listOf(listDetailStrategy),
        modifier = modifier.fillMaxSize(),
        entryProvider = entryProvider {
            entry<Home>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { EmptyChannelDetailPlaceholder() },
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
                    selectedTab = HomeTab.Channels,
                    onSelectTab = {},
                    onPlanixClick = {},
                    onSignOut = {},
                    onSelectWorkspace = {},
                    channelsContent = {
                        ChannelsContent(
                            uiState = ChannelUiState(channels = mockChannels),
                            onChannelClick = { channel ->
                                if (backStack.lastOrNull() is ChannelDetail) {
                                    backStack.removeLastOrNull()
                                }
                                backStack.add(ChannelDetail(channel.id))
                            },
                            onRefresh = {},
                        )
                    },
                    dmsContent = {
                        DmsContent(
                            uiState = DmsUiState(),
                            onDmClick = {},
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
                    onBack = { backStack.removeLastOrNull() },
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
