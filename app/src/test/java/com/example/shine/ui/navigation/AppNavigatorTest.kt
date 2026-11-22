package com.example.shine.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.shine.core.model.AuthState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigatorTest {

    @Test
    fun resetTo_clearsStackAndSetsTarget() {
        val stack = mutableStateListOf<NavKey>(SignIn)
        val navStack = NavBackStack(stack)
        val navigator = AppNavigator(navStack, authState = { AuthState.Authenticated })

        navigator.resetTo(WorkspaceList)
        assertEquals(listOf(WorkspaceList), navigator.backStack)
        assertEquals(WorkspaceList, navigator.currentKey)

        navigator.resetTo(Home)
        assertEquals(listOf(Home), navigator.backStack)
        assertEquals(Home, navigator.currentKey)
    }

    @Test
    fun navigate_respectsTypedDomainRouteGuards() {
        val stack = mutableStateListOf<NavKey>(SignIn)
        val navStack = NavBackStack(stack)
        var authState: AuthState = AuthState.Unauthenticated
        val navigator = AppNavigator(navStack, authState = { authState })

        // When Unauthenticated, cannot navigate to Authenticated or SelectWorkspace keys
        navigator.navigate(Home)
        assertEquals(listOf(SignIn), navigator.backStack)
        navigator.navigate(WorkspaceList)
        assertEquals(listOf(SignIn), navigator.backStack)

        // When SelectWorkspace, cannot navigate to Authenticated keys
        authState = AuthState.SelectWorkspace
        navigator.navigate(Home)
        assertEquals(listOf(SignIn), navigator.backStack)

        // When Authenticated, can navigate to Authenticated keys
        authState = AuthState.Authenticated
        navigator.navigate(Home)
        assertEquals(listOf(SignIn, Home), navigator.backStack)
        navigator.navigate(Profile)
        assertEquals(listOf(SignIn, Home, Profile), navigator.backStack)
    }

    @Test
    fun pop_removesTopWhenMoreThanOne() {
        val stack = mutableStateListOf<NavKey>(Home, Profile)
        val navStack = NavBackStack(stack)
        val navigator = AppNavigator(navStack, authState = { AuthState.Authenticated })

        assertTrue(navigator.pop())
        assertEquals(listOf(Home), navigator.backStack)

        assertFalse(navigator.pop())
        assertEquals(listOf(Home), navigator.backStack)
    }

    @Test
    fun popUntil_popsBackToSpecifiedKey() {
        val stack = mutableStateListOf<NavKey>(Home, PlanixList, PlanixDetail("p-1"))
        val navStack = NavBackStack(stack)
        val navigator = AppNavigator(navStack, authState = { AuthState.Authenticated })

        navigator.popUntil(Home)
        assertEquals(listOf(Home), navigator.backStack)
    }

    @Test
    fun setBackStack_respectsAuthRouteGuardsAndReplacesStack() {
        val stack = mutableStateListOf<NavKey>(Home)
        val navStack = NavBackStack(stack)
        val navigator = AppNavigator(navStack, authState = { AuthState.Authenticated })

        // When Authenticated, valid AuthenticatedNavKeys replace the stack
        navigator.setBackStack(listOf(Home, ChannelDetail("c-1")))
        assertEquals(listOf(Home, ChannelDetail("c-1")), navigator.backStack)

        // Unauthenticated keys are filtered out by guard
        navigator.setBackStack(listOf(SignIn, Home, Profile))
        assertEquals(listOf(Home, Profile), navigator.backStack)
    }

    @Test
    fun setBackStack_whenUnauthenticated_ignoresAuthenticatedKeys() {
        val stack = mutableStateListOf<NavKey>(SignIn)
        val navStack = NavBackStack(stack)
        val navigator = AppNavigator(navStack, authState = { AuthState.Unauthenticated })

        // When Unauthenticated, deep link trying to set AuthenticatedNavKeys is rejected
        navigator.setBackStack(listOf(Home, ChannelDetail("c-1")))
        assertEquals(listOf(SignIn), navigator.backStack)

        // Allowed unauthenticated keys succeed
        navigator.setBackStack(listOf(SignIn))
        assertEquals(listOf(SignIn), navigator.backStack)
    }

    @Test
    fun setBackStack_whenSelectWorkspace_ignoresAuthenticatedKeys() {
        val stack = mutableStateListOf<NavKey>(WorkspaceList)
        val navStack = NavBackStack(stack)
        val navigator = AppNavigator(navStack, authState = { AuthState.SelectWorkspace })

        navigator.setBackStack(listOf(Home, ChannelDetail("c-1")))
        assertEquals(listOf(WorkspaceList), navigator.backStack)
    }
}
