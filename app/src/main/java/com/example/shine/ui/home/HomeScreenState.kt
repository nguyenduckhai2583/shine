package com.example.shine.ui.home

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class HomeScreenState(
    val drawerState: DrawerState,
    val scope: CoroutineScope,
    initialTab: HomeTab = HomeTab.Channels,
) {
    var selectedTab by mutableStateOf(initialTab)

    fun openDrawer() {
        scope.launch { drawerState.open() }
    }

    fun closeDrawer() {
        scope.launch { drawerState.close() }
    }
}

@Composable
fun rememberHomeScreenState(
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    scope: CoroutineScope = rememberCoroutineScope(),
    initialTab: HomeTab = HomeTab.Channels,
): HomeScreenState {
    return remember(drawerState, scope) {
        HomeScreenState(drawerState = drawerState, scope = scope, initialTab = initialTab)
    }
}
