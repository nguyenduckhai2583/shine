package com.example.shine.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shine.core.designsystem.theme.MultiDevicePreviews
import com.example.shine.core.designsystem.theme.ShineTheme
import com.example.shine.core.model.Workspace
import com.example.shine.feature.home.components.HomeDrawerContent
import com.example.shine.feature.home.components.SettingsContent

@Composable
fun HomeRoute(
    channelsContent: @Composable () -> Unit,
    dmsContent: @Composable () -> Unit,
    onPlanixClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.Channels) }

    HomeScreen(
        uiState = uiState,
        selectedTab = selectedTab,
        onSelectTab = { selectedTab = it },
        onPlanixClick = onPlanixClick,
        onProfileClick = onProfileClick,
        onSignOut = viewModel::signOut,
        onSelectWorkspace = viewModel::selectWorkspace,
        channelsContent = channelsContent,
        dmsContent = dmsContent,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    selectedTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    onPlanixClick: () -> Unit,
    onSignOut: () -> Unit,
    onSelectWorkspace: (String) -> Unit,
    channelsContent: @Composable () -> Unit,
    dmsContent: @Composable () -> Unit,
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
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                item(
                    selected = selectedTab == HomeTab.Channels,
                    onClick = { onSelectTab(HomeTab.Channels) },
                    icon = { Icon(Icons.Filled.Tag, contentDescription = "Channels") },
                    label = { Text("Channels") },
                )
                item(
                    selected = selectedTab == HomeTab.Dms,
                    onClick = { onSelectTab(HomeTab.Dms) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "DMs") },
                    label = { Text("DMs") },
                )
                item(
                    selected = selectedTab == HomeTab.Settings,
                    onClick = { onSelectTab(HomeTab.Settings) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                )
            },
        ) {
            Scaffold(
                topBar = {
                    HomeTopAppBar(
                        selectedTab = selectedTab,
                        uiState = uiState,
                        onOpenDrawer = state::openDrawer,
                    )
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (uiState.isSwitchingWorkspace) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        when (selectedTab) {
                            HomeTab.Channels -> channelsContent()
                            HomeTab.Dms -> dmsContent()
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

@MultiDevicePreviews
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
            selectedTab = HomeTab.Channels,
            onSelectTab = {},
            onPlanixClick = {},
            onSignOut = {},
            onSelectWorkspace = {},
            channelsContent = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Channels Content")
                }
            },
            dmsContent = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("DMs Content")
                }
            },
        )
    }
}
