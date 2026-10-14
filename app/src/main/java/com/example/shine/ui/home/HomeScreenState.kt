package com.example.shine.ui.home

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class HomeScreenState(
    val drawerState: DrawerState,
    val scope: CoroutineScope,
) {
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
): HomeScreenState {
    return remember(drawerState, scope) {
        HomeScreenState(drawerState = drawerState, scope = scope)
    }
}
