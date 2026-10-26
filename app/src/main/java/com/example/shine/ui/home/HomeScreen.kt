package com.example.shine.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shine.domain.model.Channel
import com.example.shine.domain.model.Workspace
import com.example.shine.ui.home.components.ChannelsRoute
import com.example.shine.ui.home.components.DmsContent
import com.example.shine.ui.home.components.HomeDrawerContent
import com.example.shine.ui.home.components.SettingsContent
import com.example.shine.ui.theme.ShineTheme

@Composable
fun HomeRoute(
    onChannelClick: (Channel) -> Unit,
    onPlanixClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onChannelClick = onChannelClick,
        onPlanixClick = onPlanixClick,
        onProfileClick = onProfileClick,
        onSignOut = viewModel::signOut,
        onSelectWorkspace = viewModel::selectWorkspace,
        onSelectTab = viewModel::selectTab,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onChannelClick: (Channel) -> Unit,
    onPlanixClick: () -> Unit,
    onSignOut: () -> Unit,
    onSelectWorkspace: (String) -> Unit,
    onSelectTab: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {},
    state: HomeScreenState = rememberHomeScreenState(),
) {
    ModalNavigationDrawer(
        drawerState = state.drawerState,
        modifier = modifier,
        drawerContent = {
            HomeDrawerContent(
                uiState = uiState,
                onSelectWorkspace = { workspaceId ->
                    state.closeDrawer()
                    onSelectWorkspace(workspaceId)
                },
                onSignOut = {
                    state.closeDrawer()
                    onSignOut()
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                HomeTopAppBar(
                    selectedTab = uiState.selectedTab,
                    uiState = uiState,
                    onOpenDrawer = state::openDrawer,
                )
            },
            bottomBar = {
                HomeBottomBar(
                    selectedTab = uiState.selectedTab,
                    onSelectTab = onSelectTab,
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (uiState.selectedTab) {
                    HomeTab.Channels -> ChannelsRoute(onChannelClick = onChannelClick)
                    HomeTab.Dms -> DmsContent()
                    HomeTab.Settings -> SettingsContent(
                        uiState = uiState,
                        onPlanixClick = onPlanixClick,
                        onSignOut = onSignOut,
                        onProfileClick = onProfileClick,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopAppBar(
    selectedTab: HomeTab,
    uiState: HomeUiState,
    onOpenDrawer: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = "Open workspaces drawer")
            }
        },
        title = {
            Column {
                when (selectedTab) {
                    HomeTab.Channels -> {
                        Text("Channels")
                        val subtitle = uiState.currentWorkspaceName.ifEmpty { uiState.displayName }
                        if (subtitle.isNotEmpty()) {
                            Text(subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    HomeTab.Dms -> Text("DMs")
                    HomeTab.Settings -> Text("Settings")
                }
            }
        },
    )
}

@Composable
private fun HomeBottomBar(
    selectedTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == HomeTab.Channels,
            onClick = { onSelectTab(HomeTab.Channels) },
            icon = { Icon(Icons.Filled.Tag, contentDescription = "Channels") },
            label = { Text("Channels") },
        )
        NavigationBarItem(
            selected = selectedTab == HomeTab.Dms,
            onClick = { onSelectTab(HomeTab.Dms) },
            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "DMs") },
            label = { Text("DMs") },
        )
        NavigationBarItem(
            selected = selectedTab == HomeTab.Settings,
            onClick = { onSelectTab(HomeTab.Settings) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ShineTheme(dynamicColor = false) {
        HomeScreen(
            uiState = HomeUiState(
                displayName = "Jane Doe",
                workspaces = listOf(
                    Workspace("1", "Default Workspace", true, "default"),
                ),
                selectedWorkspaceId = "1",
            ),
            onChannelClick = {},
            onPlanixClick = {},
            onSignOut = {},
            onSelectWorkspace = {},
            onSelectTab = {},
        )
    }
}
