package com.example.shine.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.R
import com.example.shine.domain.model.AppException
import com.example.shine.ui.common.UiText
import com.example.shine.ui.common.toUiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    /**
     * Executes a task returning [Result].
     * - On success, calls [onSuccess] with the resulting data.
     * - On failure, converts the error message into a [UiText] and calls [onError].
     * - [onFinally] always runs at the end (e.g. to dismiss dialogs, reset loading).
     */
    protected fun <T> execute(
        task: suspend () -> Result<T>,
        onSuccess: (T) -> Unit = {},
        onError: (UiText) -> Unit = {},
        onFinally: () -> Unit = {},
    ): Job = viewModelScope.launch {
        try {
            task()
                .onSuccess(onSuccess)
                .onFailure { error ->
                    val message = when (error) {
                        is AppException -> error.toUiText()
                        else -> error.message?.let { UiText(it) } ?: UiText(R.string.error_unknown)
                    }
                    onError(message)
                }
        } finally {
            onFinally()
        }
    }
}

