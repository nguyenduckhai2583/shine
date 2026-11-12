package com.example.shine.feature.camera

enum class CameraLens {
    BACK,
    FRONT,
}

data class CameraUiState(
    val captureMode: CameraCaptureMode = CameraCaptureMode.PHOTO,
    val allowedModes: Set<CameraCaptureMode> = setOf(CameraCaptureMode.PHOTO, CameraCaptureMode.VIDEO),
    val currentLens: CameraLens = CameraLens.BACK,
    val availableLenses: List<CameraLens> = listOf(CameraLens.BACK),
    val isRecording: Boolean = false,
    val recordingDurationSeconds: Int = 0,
    val hasAudioPermission: Boolean = true,
)
