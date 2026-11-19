package com.example.shine.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.shine.core.model.AuthState

/**
 * State holder that encapsulates backstack operations with route protection guards.
 */
class AppNavigator(
    val backStack: NavBackStack<NavKey>,
    val authState: () -> AuthState,
) {
    val currentKey: NavKey?
        get() = backStack.lastOrNull()

    /**
     * Navigates to the specified [key] with route protection checks.
     */
    fun navigate(key: NavKey) {
        val isAllowed = when (authState()) {
            AuthState.Authenticated -> key is AuthenticatedNavKey
            AuthState.SelectWorkspace -> key is SelectWorkspaceNavKey
            AuthState.Unauthenticated -> key is UnauthenticatedNavKey
            AuthState.Initializing -> false
        }
        if (isAllowed) {
            backStack.add(key)
        }
    }

    /**
     * Pops the top destination if backstack size > 1 (retaining the root destination).
     */
    fun pop(): Boolean {
        return backStack.size > 1 && backStack.removeLastOrNull() != null
    }

    /**
     * Clears the backstack and sets [key] as the sole destination.
     */
    fun resetTo(key: NavKey) {
        backStack.clear()
        backStack.add(key)
    }

    /**
     * Pops destinations until [key] is reached at the top.
     * If [key] does not exist in the stack, no operation is performed.
     */
    fun popUntil(key: NavKey) {
        if (key !in backStack) return
        while (backStack.isNotEmpty() && backStack.last() != key) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun removeAll(predicate: (NavKey) -> Boolean) {
        backStack.removeAll(predicate)
    }
}
