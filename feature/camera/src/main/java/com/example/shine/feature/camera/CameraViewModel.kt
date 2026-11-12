package com.example.shine.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.shine.core.common.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun initialize(allowedModes: Set<CameraCaptureMode>, hasAudioPermission: Boolean) {
        _uiState.update {
            val safeModes = allowedModes.ifEmpty { setOf(CameraCaptureMode.PHOTO) }
            val currentMode = if (it.captureMode in safeModes) it.captureMode else safeModes.first()
            it.copy(
                allowedModes = safeModes,
                captureMode = currentMode,
                hasAudioPermission = hasAudioPermission,
            )
        }
    }

    fun setAvailableLenses(lenses: List<CameraLens>) {
        _uiState.update { current ->
            val updatedLenses = lenses.ifEmpty { listOf(CameraLens.BACK) }
            val currentLens = if (current.currentLens in updatedLenses) current.currentLens else updatedLenses.first()
            current.copy(
                availableLenses = updatedLenses,
                currentLens = currentLens,
            )
        }
    }

    fun onModeSelected(mode: CameraCaptureMode) {
        if (_uiState.value.isRecording) return
        if (mode in _uiState.value.allowedModes) {
            _uiState.update { it.copy(captureMode = mode) }
        }
    }

    fun onFlipCamera() {
        if (_uiState.value.isRecording) return
        _uiState.update { current ->
            val lenses = current.availableLenses
            if (lenses.isEmpty()) return@update current
            val currentIndex = lenses.indexOf(current.currentLens)
            val nextIndex = (currentIndex + 1) % lenses.size
            current.copy(currentLens = lenses[nextIndex])
        }
    }

    fun onRecordingStarted() {
        _uiState.update { it.copy(isRecording = true, recordingDurationSeconds = 0) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch(defaultDispatcher) {
            while (isActive) {
                delay(1000.milliseconds)
                _uiState.update { it.copy(recordingDurationSeconds = it.recordingDurationSeconds + 1) }
            }
        }
    }

    fun onRecordingStopped() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isRecording = false) }
    }

    override fun onCleared() {
        timerJob?.cancel()
        timerJob = null
    }
}
