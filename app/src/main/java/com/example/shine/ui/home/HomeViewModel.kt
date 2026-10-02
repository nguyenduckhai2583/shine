package com.example.shine.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.domain.usecase.ObserveSessionUseCase
import com.example.shine.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(val displayName: String = "")

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = observeSession()
        .map { HomeUiState(displayName = it?.user?.displayName.orEmpty()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun signOut() {
        viewModelScope.launch { signOutUseCase() }
    }
}
