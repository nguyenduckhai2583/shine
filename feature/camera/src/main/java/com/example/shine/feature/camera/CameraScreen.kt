package com.example.shine.feature.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shine.core.common.permission.AppPermission
import com.example.shine.core.common.permission.PermissionStatus
import com.example.shine.core.common.permission.rememberPermissionState

@Composable
fun CameraRoute(
    viewModel: CameraViewModel,
    onMediaCaptured: (CapturedMedia) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    allowedModes: Set<CameraCaptureMode> = setOf(CameraCaptureMode.PHOTO, CameraCaptureMode.VIDEO),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CameraScreen(
        uiState = uiState,
        onModeSelected = viewModel::onModeSelected,
        onFlipCamera = viewModel::onFlipCamera,
        onAvailableLensesDetected = viewModel::setAvailableLenses,
        onRecordingStarted = viewModel::onRecordingStarted,
        onRecordingStopped = viewModel::onRecordingStopped,
        onInitialize = viewModel::initialize,
        onMediaCaptured = onMediaCaptured,
        onClose = onClose,
        modifier = modifier,
        allowedModes = allowedModes,
    )
}

@Composable
fun CameraScreen(
    uiState: CameraUiState,
    onModeSelected: (CameraCaptureMode) -> Unit,
    onFlipCamera: () -> Unit,
    onAvailableLensesDetected: (List<CameraLens>) -> Unit,
    onRecordingStarted: () -> Unit,
    onRecordingStopped: () -> Unit,
    onInitialize: (Set<CameraCaptureMode>) -> Unit,
    onMediaCaptured: (CapturedMedia) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    allowedModes: Set<CameraCaptureMode> = setOf(CameraCaptureMode.PHOTO, CameraCaptureMode.VIDEO),
) {
    val appPermission = remember(allowedModes) {
        if (allowedModes.contains(CameraCaptureMode.VIDEO)) {
            AppPermission.Video
        } else {
            AppPermission.Camera
        }
    }
    val permissionState = rememberPermissionState(appPermission)
    val isGranted = permissionState.status == PermissionStatus.Granted

    LaunchedEffect(allowedModes, isGranted) {
        if (isGranted) {
            onInitialize(allowedModes)
        }
    }

    LaunchedEffect(Unit) {
        if (!isGranted) {
            permissionState.request()
        }
    }

    when (val status = permissionState.status) {
        PermissionStatus.Idle -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black),
            )
        }

        PermissionStatus.Granted -> {
            CameraView(
                uiState = uiState,
                onModeSelected = onModeSelected,
                onFlipCamera = onFlipCamera,
                onAvailableLensesDetected = onAvailableLensesDetected,
                onRecordingStarted = onRecordingStarted,
                onRecordingStopped = onRecordingStopped,
                onMediaCaptured = onMediaCaptured,
                onClose = onClose,
                modifier = modifier,
            )
        }

        PermissionStatus.Denied,
        PermissionStatus.PermanentDenied -> {
            val isPermanent = status == PermissionStatus.PermanentDenied
            CameraPermissionDeniedWidget(
                onClose = onClose,
                modifier = modifier,
                isPermanent = isPermanent,
                onAction = {
                    if (isPermanent) {
                        permissionState.openSettings()
                    } else {
                        permissionState.request()
                    }
                },
            )
        }
    }
}
