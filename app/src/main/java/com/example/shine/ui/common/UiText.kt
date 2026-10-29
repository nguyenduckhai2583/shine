package com.example.shine.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.shine.R
import com.example.shine.domain.model.AppErrorEnum
import com.example.shine.domain.model.AppException

sealed interface UiText {
    data class Raw(val value: String) : UiText

    data class Resource(
        @StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText {
        constructor(@StringRes id: Int, vararg args: Any) : this(id, args.toList())
    }

    @Composable
    fun asString(): String = when (this) {
        is Raw -> value
        is Resource -> if (args.isEmpty()) stringResource(id) else stringResource(id, *args.toTypedArray())
    }

    fun asString(context: Context): String = when (this) {
        is Raw -> value
        is Resource -> if (args.isEmpty()) context.getString(id) else context.getString(id, *args.toTypedArray())
    }

    companion object {
        operator fun invoke(value: String): UiText = Raw(value)
        operator fun invoke(@StringRes id: Int, vararg args: Any): UiText = Resource(id, *args)
    }
}

fun AppException.toUiText(): UiText =
    errorMessage?.let { UiText.Raw(it) } ?: UiText.Resource(type.toStringRes())

@StringRes
private fun AppErrorEnum.toStringRes(): Int = when (this) {
    AppErrorEnum.NO_INTERNET -> R.string.error_no_internet
    AppErrorEnum.SERVER -> R.string.error_server
    AppErrorEnum.UNKNOWN -> R.string.error_unknown
}
