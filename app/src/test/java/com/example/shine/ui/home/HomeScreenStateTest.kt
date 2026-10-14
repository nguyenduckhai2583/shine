package com.example.shine.ui.home

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenStateTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun openAndCloseDrawer_updatesDrawerState() = runTest(testDispatcher) {
        val drawerState = DrawerState(DrawerValue.Closed)
        val state = HomeScreenState(
            drawerState = drawerState,
            scope = this,
        )

        state.openDrawer()
        testScheduler.advanceUntilIdle()
        assertEquals(DrawerValue.Open, drawerState.currentValue)

        state.closeDrawer()
        testScheduler.advanceUntilIdle()
        assertEquals(DrawerValue.Closed, drawerState.currentValue)
    }
}
