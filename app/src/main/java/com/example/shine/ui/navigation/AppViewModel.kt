package com.example.shine.ui.navigation

import android.content.Intent
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.example.shine.core.common.base.BaseViewModel
import com.example.shine.core.common.network.NetworkMonitor
import com.example.shine.core.datastore.SessionManager
import com.example.shine.core.data.session.SessionPreloader
import com.example.shine.core.model.AuthState
import com.example.shine.ui.navigation.deeplink.AppDeepLinkHandler
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
    private val deepLinkHandler: AppDeepLinkHandler,
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

    private val _deepLinkBackStack = MutableStateFlow<List<NavKey>?>(null)
    val deepLinkBackStack: StateFlow<List<NavKey>?> = _deepLinkBackStack.asStateFlow()

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

    fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val backStack = deepLinkHandler.handleIntent(intent)
        if (backStack != null) {
            _deepLinkBackStack.value = backStack
        }
    }

    fun onDeepLinkConsumed() {
        _deepLinkBackStack.value = null
    }
}
