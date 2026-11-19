package com.example.shine.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.shine.core.common.base.BaseViewModel
import com.example.shine.core.common.network.NetworkMonitor
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.data.session.SessionPreloader
import com.example.shine.core.model.AuthState
import com.example.shine.core.shortcut.AppShortcutManager
import com.example.shine.ui.navigation.deeplink.DeepLinkDestination
import com.example.shine.ui.navigation.deeplink.DeepLinkParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    sessionManager: SessionManager,
    private val sessionPreloader: SessionPreloader,
    private val deepLinkParser: DeepLinkParser,
    networkMonitor: NetworkMonitor,
) : BaseViewModel() {

    val authState: StateFlow<AuthState> = sessionManager.authState

    val isOffline: StateFlow<Boolean> = networkMonitor.isOnline
        .map { !it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    private val _pendingDestination = MutableStateFlow<DeepLinkDestination?>(null)
    val pendingDestination: StateFlow<DeepLinkDestination?> = _pendingDestination.asStateFlow()

    init {
        viewModelScope.launch {
            authState.collect { state ->
                when (state) {
                    AuthState.Authenticated -> sessionPreloader.preloadIfNeeded()
                    AuthState.Unauthenticated -> sessionPreloader.reset()
                    AuthState.SelectWorkspace,
                    AuthState.Initializing -> Unit
                }
            }
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
