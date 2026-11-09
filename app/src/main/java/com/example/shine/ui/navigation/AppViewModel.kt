package com.example.shine.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.shine.core.base.BaseViewModel
import com.example.shine.core.shortcut.AppShortcutManager
import com.example.shine.core.util.NetworkMonitor
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.domain.repository.UserRepository
import com.example.shine.ui.navigation.deeplink.DeepLinkDestination
import com.example.shine.ui.navigation.deeplink.DeepLinkParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

sealed interface AuthState {
    data object Initializing : AuthState
    data object Unauthenticated : AuthState
    data object SelectWorkspace : AuthState
    data object Authenticated : AuthState
}

@HiltViewModel
class AppViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val deepLinkParser: DeepLinkParser,
    networkMonitor: NetworkMonitor,
) : BaseViewModel() {

    val isOffline: StateFlow<Boolean> = networkMonitor.isOnline
        .map { !it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    private val _pendingDestination = MutableStateFlow<DeepLinkDestination?>(null)
    val pendingDestination: StateFlow<DeepLinkDestination?> = _pendingDestination.asStateFlow()

    private var hasPreloaded = false

    val authState: StateFlow<AuthState> = authRepository.session
        .transform { session ->
            when {
                session == null -> {
                    hasPreloaded = false
                    emit(AuthState.Unauthenticated)
                }
                session.workspaceId.isNullOrEmpty() -> {
                    hasPreloaded = false
                    emit(AuthState.SelectWorkspace)
                }
                else -> {
                    preloadInitialDataIfNeeded()
                    emit(AuthState.Authenticated)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Initializing)

    private suspend fun preloadInitialDataIfNeeded() {
        if (!hasPreloaded) {
            supervisorScope {
                awaitAll(
                    async { runCatching { userRepository.getProfile() } },
                    async { runCatching { userRepository.getChatUsers() } },
                )
            }
            hasPreloaded = true
        }
    }

    fun handleDeepLink(uri: Uri?) {
        val destination = deepLinkParser.parse(uri)
        if (destination != null) {
            _pendingDestination.value = destination
        }
    }

    fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val shortcutAction = intent.getStringExtra(AppShortcutManager.EXTRA_SHORTCUT_ACTION)
        val channelId = intent.getStringExtra(AppShortcutManager.EXTRA_CHANNEL_ID)

        when {
            shortcutAction == AppShortcutManager.ACTION_OPEN_PROFILE -> {
                _pendingDestination.value = DeepLinkDestination.Profile
            }
            !channelId.isNullOrBlank() -> {
                _pendingDestination.value = DeepLinkDestination.Channel(channelId = channelId)
            }
            intent.data != null -> {
                handleDeepLink(intent.data)
            }
        }
    }

    fun onDeepLinkConsumed() {
        _pendingDestination.value = null
    }
}
