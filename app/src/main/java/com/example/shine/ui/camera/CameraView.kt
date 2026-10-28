package com.example.shine.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CameraView(
    onMediaCaptured: (CapturedMedia) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    allowedModes: Set<CameraCaptureMode> = setOf(CameraCaptureMode.PHOTO, CameraCaptureMode.VIDEO),
    hasAudioPermission: Boolean = true,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var captureMode by rememberSaveable { mutableStateOf(allowedModes.first()) }
    var availableCameras by remember { mutableStateOf<List<CameraSelector>>(emptyList()) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

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
        }
    }

    LaunchedEffect(Unit) {
        val cameraProvider = runCatching {
            ProcessCameraProvider.getInstance(context).get()
        }.getOrNull() ?: return@LaunchedEffect

        availableCameras = buildList {
            if (runCatching { cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false)) {
                add(CameraSelector.DEFAULT_BACK_CAMERA)
            }
            if (runCatching { cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false)) {
                add(CameraSelector.DEFAULT_FRONT_CAMERA)
            }
            if (isEmpty()) {
                add(CameraSelector.DEFAULT_BACK_CAMERA)
            }
        }
        if (cameraSelector !in availableCameras) {
            cameraSelector = availableCameras.first()
        }
    }

    LaunchedEffect(cameraSelector) {
        val cameraProvider = runCatching {
            ProcessCameraProvider.getInstance(context).get()
        }.getOrNull() ?: return@LaunchedEffect

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
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
            val fallbackUseCase = if (captureMode == CameraCaptureMode.PHOTO) imageCapture else videoCapture
            runCatching {
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, fallbackUseCase)
            }
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSeconds = 0
            while (isActive && isRecording) {
                delay(1000L.milliseconds)
                recordingDurationSeconds++
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
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )

            if (isRecording) {
                CameraRecordingIndicator(
                    durationSeconds = recordingDurationSeconds,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp),
                )
            }

            CameraShutterButton(
                captureMode = captureMode,
                isRecording = isRecording,
                onClick = {
                    if (captureMode == CameraCaptureMode.PHOTO) {
                        takePhoto(
                            context = context,
                            imageCapture = imageCapture,
                            onPhotoSaved = { uri ->
                                onMediaCaptured(CapturedMedia.Photo(uri))
                            },
                        )
                    } else {
                        if (isRecording) {
                            activeRecording?.stop()
                            activeRecording = null
                            isRecording = false
                        } else {
                            val recording = startVideoRecording(
                                context = context,
                                videoCapture = videoCapture,
                                hasAudioPermission = hasAudioPermission,
                                onVideoSaved = { uri, durationMs ->
                                    onMediaCaptured(CapturedMedia.Video(uri, durationMs))
                                },
                            )
                            activeRecording = recording
                            isRecording = true
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
            )
        }

        CameraBottomBar(
            captureMode = captureMode,
            allowedModes = allowedModes,
            isRecording = isRecording,
            onClose = onClose,
            onModeSelected = { newMode ->
                if (!isRecording) {
                    captureMode = newMode
                }
            },
            onFlipCamera = {
                if (availableCameras.isNotEmpty()) {
                    val nextIndex = (availableCameras.indexOf(cameraSelector) + 1) % availableCameras.size
                    cameraSelector = availableCameras[nextIndex]
                }
            },
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
