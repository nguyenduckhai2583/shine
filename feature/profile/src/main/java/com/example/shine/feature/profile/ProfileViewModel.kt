package com.example.shine.feature.profile

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.shine.core.common.base.BaseViewModel
import com.example.shine.core.data.repository.AuthRepository
import com.example.shine.core.data.repository.UserRepository
import com.example.shine.core.common.ui.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface UploadState {
    data object Idle : UploadState
    data class Uploading(val selectedUri: Uri) : UploadState
    data class Error(val message: UiText) : UploadState
}

data class ProfileUiState(
    val name: String = "",
    val avatarUrl: String? = null,
    val isLoading: Boolean = false,
    val uploadState: UploadState = UploadState.Idle,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : BaseViewModel() {

    private val uploadState = MutableStateFlow<UploadState>(UploadState.Idle)

    val uiState: StateFlow<ProfileUiState> = combine(
        authRepository.session,
        uploadState,
    ) { session, upload ->
        val user = session?.user
        ProfileUiState(
            name = user?.displayName.orEmpty(),
            avatarUrl = user?.avatarUrl,
            isLoading = session == null,
            uploadState = upload,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(isLoading = true),
    )

    fun onAvatarSelected(uri: Uri) {
        uploadState.value = UploadState.Uploading(uri)
        execute(
            task = { userRepository.updateAvatar(uri) },
            onSuccess = {
                uploadState.value = UploadState.Idle
            },
            onError = { error ->
                uploadState.value = UploadState.Error(error)
            },
        )
    }

    fun clearErrorMessage() {
        if (uploadState.value is UploadState.Error) {
            uploadState.value = UploadState.Idle
        }
    }
}
