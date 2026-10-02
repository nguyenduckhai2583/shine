package com.example.shine.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.shine.R
import com.example.shine.domain.model.AppError
import com.example.shine.domain.model.AppException

sealed interface UiText {
    data class Raw(val value: String) : UiText

    data class Resource(@StringRes val id: Int) : UiText

    @Composable
    fun asString(): String = when (this) {
        is Raw -> value
        is Resource -> stringResource(id)
    }
}

fun AppException.toUiText(): UiText =
    serverMessage?.let { UiText.Raw(it) } ?: UiText.Resource(error.toStringRes())

@StringRes
private fun AppError.toStringRes(): Int = when (this) {
    AppError.NO_INTERNET -> R.string.error_no_internet
    AppError.SERVER -> R.string.error_server
    AppError.TWO_FACTOR_REQUIRED -> R.string.error_two_factor_required
    AppError.UNKNOWN -> R.string.error_unknown
}
