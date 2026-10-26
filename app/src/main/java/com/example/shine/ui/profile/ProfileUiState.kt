package com.example.shine.ui.profile

import android.net.Uri

data class ProfileUiState(
    val name: String = "",
    val avatarUrl: String? = null,
    val selectedAvatarUri: Uri? = null,
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val errorMessage: String? = null,
)
