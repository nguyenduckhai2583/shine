package com.example.shine.core.common.base

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shine.core.common.R
import com.example.shine.core.common.error.AppException
import com.example.shine.core.common.ui.UiText
import com.example.shine.core.common.ui.toUiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    private val tag: String = this::class.java.simpleName
    private val instanceId: String = Integer.toHexString(System.identityHashCode(this))

    init {
        Log.d(TAG, "🟢 [$tag@$instanceId] INITIALIZED")
    }

    override fun onCleared() {
        Log.d(TAG, "🔴 [$tag@$instanceId] CLEARED")
    }

    protected fun <T> execute(
        task: suspend () -> Result<T>,
        onSuccess: (T) -> Unit = {},
        onError: (UiText) -> Unit = {},
        onComplete: () -> Unit = {},
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
            onComplete()
        }
    }

    companion object {
        private const val TAG = "ViewModelLifecycle"
    }
}
