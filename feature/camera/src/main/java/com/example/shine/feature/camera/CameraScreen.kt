package com.example.shine.feature.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CameraRoute(
    onMediaCaptured: (CapturedMedia) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    allowedModes: Set<CameraCaptureMode> = setOf(CameraCaptureMode.PHOTO, CameraCaptureMode.VIDEO),
    viewModel: CameraViewModel = hiltViewModel(),
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
    onInitialize: (Set<CameraCaptureMode>, Boolean) -> Unit,
    onMediaCaptured: (CapturedMedia) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    allowedModes: Set<CameraCaptureMode> = setOf(CameraCaptureMode.PHOTO, CameraCaptureMode.VIDEO),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        permissionRequested = true
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED
                hasAudioPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(allowedModes, hasAudioPermission) {
        onInitialize(allowedModes, hasAudioPermission)
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = buildList {
            if (!hasCameraPermission) {
                add(Manifest.permission.CAMERA)
            }
            if (!hasAudioPermission && allowedModes.contains(CameraCaptureMode.VIDEO)) {
                add(Manifest.permission.RECORD_AUDIO)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionDeniedWidget(
            onClose = onClose,
            modifier = modifier,
            showContent = permissionRequested,
        )
        return
    }

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
