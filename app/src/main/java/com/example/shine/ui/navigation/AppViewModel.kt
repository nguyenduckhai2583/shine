package com.example.shine.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.domain.usecase.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data object SignedIn : AuthState
}

@HiltViewModel
class AppViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
) : ViewModel() {

    val authState: StateFlow<AuthState> = observeSession()
        .map { session -> if (session == null) AuthState.SignedOut else AuthState.SignedIn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)
}
