package com.example.shine.ui.navigation

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.shine.core.base.BaseViewModel
import com.example.shine.domain.repository.AuthRepository
import com.example.shine.domain.repository.UserRepository
import com.example.shine.ui.navigation.deeplink.DeepLinkDestination
import com.example.shine.ui.navigation.deeplink.DeepLinkParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data object SelectWorkspace : AuthState
    data object SignedIn : AuthState
}

@HiltViewModel
class AppViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val deepLinkParser: DeepLinkParser,
) : BaseViewModel() {

    private val _pendingDestination = MutableStateFlow<DeepLinkDestination?>(null)
    val pendingDestination: StateFlow<DeepLinkDestination?> = _pendingDestination.asStateFlow()

    val authState: StateFlow<AuthState> = flow {
        val initialSession = authRepository.session.first()
        if (initialSession != null) {
            userRepository.getProfile()
        }
        emitAll(
            authRepository.session.map { session ->
                when {
                    session == null -> AuthState.SignedOut
                    session.workspaceId.isNullOrEmpty() -> AuthState.SelectWorkspace
                    else -> AuthState.SignedIn
                }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    fun handleDeepLink(uri: Uri?) {
        val destination = deepLinkParser.parse(uri)
        if (destination != null) {
            _pendingDestination.value = destination
        }
    }

    fun onDeepLinkConsumed() {
        _pendingDestination.value = null
    }
}
