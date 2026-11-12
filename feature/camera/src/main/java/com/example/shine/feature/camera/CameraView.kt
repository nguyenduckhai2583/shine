package com.example.shine.feature.camera

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraView(
    uiState: CameraUiState,
    onModeSelected: (CameraCaptureMode) -> Unit,
    onFlipCamera: () -> Unit,
    onAvailableLensesDetected: (List<CameraLens>) -> Unit,
    onRecordingStarted: () -> Unit,
    onRecordingStopped: () -> Unit,
    onMediaCaptured: (CapturedMedia) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraSelector = remember(uiState.currentLens) {
        if (uiState.currentLens == CameraLens.FRONT) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val videoRecorder = remember {
        val qualitySelector = QualitySelector.from(
            Quality.HIGHEST,
            FallbackStrategy.higherQualityOrLowerThan(Quality.SD),
        )
        Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(videoRecorder) }

    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.stop()
            activeRecording = null
            onRecordingStopped()
        }
    }

    LaunchedEffect(Unit) {
        val cameraProvider = runCatching {
            ProcessCameraProvider.getInstance(context).get()
        }.getOrNull() ?: return@LaunchedEffect

        val detectedLenses = buildList {
            if (runCatching { cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false)) {
                add(CameraLens.BACK)
            }
            if (runCatching { cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false)) {
                add(CameraLens.FRONT)
            }
            if (isEmpty()) {
                add(CameraLens.BACK)
            }
        }
        onAvailableLensesDetected(detectedLenses)
    }

    LaunchedEffect(cameraSelector) {
        val cameraProvider = runCatching {
            ProcessCameraProvider.getInstance(context).get()
        }.getOrNull() ?: return@LaunchedEffect

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider { request ->
                surfaceRequest = request
            }
        }

        cameraProvider.unbindAll()
        try {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture,
            )
        } catch (_: Exception) {
            val fallbackUseCase = if (uiState.captureMode == CameraCaptureMode.PHOTO) imageCapture else videoCapture
            runCatching {
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, fallbackUseCase)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            surfaceRequest?.let { request ->
                CameraXViewfinder(
                    surfaceRequest = request,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (uiState.isRecording) {
                CameraRecordingIndicator(
                    durationSeconds = uiState.recordingDurationSeconds,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp),
                )
            }

            CameraShutterButton(
                captureMode = uiState.captureMode,
                isRecording = uiState.isRecording,
                onClick = {
                    if (uiState.captureMode == CameraCaptureMode.PHOTO) {
                        takePhoto(
                            context = context,
                            imageCapture = imageCapture,
                            onPhotoSaved = { uri ->
                                onMediaCaptured(CapturedMedia.Photo(uri))
                            },
                        )
                    } else {
                        if (uiState.isRecording) {
                            activeRecording?.stop()
                            activeRecording = null
                            onRecordingStopped()
                        } else {
                            val recording = startVideoRecording(
                                context = context,
                                videoCapture = videoCapture,
                                hasAudioPermission = uiState.hasAudioPermission,
                                onVideoSaved = { uri, durationMs ->
                                    onMediaCaptured(CapturedMedia.Video(uri, durationMs))
                                },
                            )
                            activeRecording = recording
                            onRecordingStarted()
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
            )
        }

        CameraBottomBar(
            captureMode = uiState.captureMode,
            allowedModes = uiState.allowedModes,
            isRecording = uiState.isRecording,
            onClose = onClose,
            onModeSelected = onModeSelected,
            onFlipCamera = onFlipCamera,
        )
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onPhotoSaved: (Uri) -> Unit,
) {
    val photoFile = CameraMediaUtils.createPhotoFile(context)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    val executor = ContextCompat.getMainExecutor(context)

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val uri = CameraMediaUtils.getFileUri(context, photoFile)
                onPhotoSaved(uri)
            }

            override fun onError(exception: ImageCaptureException) {
                runCatching { photoFile.delete() }
            }
        },
    )
}

@SuppressLint("MissingPermission")
private fun startVideoRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    hasAudioPermission: Boolean,
    onVideoSaved: (Uri, Long) -> Unit,
): Recording {
    val videoFile = CameraMediaUtils.createVideoFile(context)
    val outputOptions = FileOutputOptions.Builder(videoFile).build()
    val executor = ContextCompat.getMainExecutor(context)

    val pendingRecording = videoCapture.output
        .prepareRecording(context, outputOptions)
        .run {
            if (hasAudioPermission) withAudioEnabled() else this
        }

    return pendingRecording.start(executor) { recordEvent ->
        when (recordEvent) {
            is VideoRecordEvent.Finalize -> {
                if (!recordEvent.hasError()) {
                    val uri = CameraMediaUtils.getFileUri(context, videoFile)
                    val durationMs = CameraMediaUtils.getVideoDuration(context, uri)
                    onVideoSaved(uri, durationMs)
                } else {
                    runCatching { videoFile.delete() }
                }
            }
            else -> Unit
        }
    }
}
