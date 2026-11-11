package com.example.shine.core.common.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.shine.core.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessAppLifecycleMonitor @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
) : AppLifecycleMonitor {

    private val _isInForeground = MutableStateFlow(
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
    )
    override val isInForeground: StateFlow<Boolean> = _isInForeground.asStateFlow()

    init {
        scope.launch(Dispatchers.Main.immediate) {
            val observer = object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    _isInForeground.update { true }
                }

                override fun onStop(owner: LifecycleOwner) {
                    _isInForeground.update { false }
                }
            }
            ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        }
    }
}
