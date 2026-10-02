package com.example.shine.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.shine.ui.channel.ChannelDetailRoute
import com.example.shine.ui.home.HomeRoute
import com.example.shine.ui.signin.SignInRoute

/**
 * Login is mandatory, so signed-out and signed-in flows are separate back stacks.
 * A successful sign-in persists the session, [AppViewModel] observes it and swaps to Home,
 * so Back from Home exits the app instead of returning to Sign in.
 */
@Composable
fun ShineApp(viewModel: AppViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    when (authState) {
        AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        AuthState.SignedOut -> AuthNavigation()
        AuthState.SignedIn -> MainNavigation()
    }
}

@Composable
private fun AuthNavigation() {
    val backStack = rememberNavBackStack(SignIn)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<SignIn> { SignInRoute() }
        },
    )
}

@Composable
private fun MainNavigation() {
    val backStack = rememberNavBackStack(Home)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Home> {
                HomeRoute(onChannelClick = { channel -> backStack.add(ChannelDetail(channel.id)) })
            }
            entry<ChannelDetail> { key ->
                ChannelDetailRoute(
                    channelId = key.channelId,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
